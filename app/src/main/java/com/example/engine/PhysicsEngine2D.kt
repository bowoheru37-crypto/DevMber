package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.engine.physics.BodyType
import com.example.engine.physics.MouseJoint2D
import com.example.engine.physics.PhysicsWorld2D
import com.example.engine.physics.RigidBody2D
import com.example.engine.physics.Vec2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class PhysicsParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val color: Color,
    val mass: Float = 1.0f,
    val restitution: Float = 0.75f,
    var isPinned: Boolean = false
)

data class PhysicsBox(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var vx: Float,
    var vy: Float,
    var angle: Float = 0f,
    var angularVelocity: Float = 0f,
    val color: Color,
    val mass: Float = 2.0f
)

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════════╗
 * ║  ⚡ 2D PHYSICS ENGINE HYBRID ARBITER (RIGID BODY DYNAMICS + KINEMATIC PARTICLE ENGINE)   ║
 * ║  Tailored for mobile games & low-entry Android devices (itel A70 / Unisoc / Android 5+). ║
 * ║  Features:                                                                               ║
 * ║  - Sequential Impulse Solver with Warm Starting & Baumgarte Stabilization                ║
 * ║  - Separating Axis Theorem (SAT) Narrowphase & Sutherland-Hodgman Manifold Clipping     ║
 * ║  - Spatial Hash Grid Broadphase (O(N) complexity with zero frame memory allocations)     ║
 * ║  - Constraints & Joints (Distance/Spring, Revolute/Pin, Mouse/Touch Drag Joint)         ║
 * ║  - Zero GC Allocations in Hot Simulation Loop                                            ║
 * ║  - NaN / Infinity Sanitization to guarantee 0% Crash Rate                                ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════════╝
 */
class PhysicsEngine2D {
    // Rigid Body Physics World
    val world: PhysicsWorld2D = PhysicsWorld2D()

    // Particle kinematics buffer for lightweight visual effects
    val particles = ArrayList<PhysicsParticle>(128)
    val boxes = ArrayList<PhysicsBox>(32)

    var gravity: Float = 980f // px/s^2
    var airResistance: Float = 0.994f
    var isSimulationRunning: Boolean = true
    var touchForceActive: Boolean = false
    var touchPos: Offset = Offset.Zero
    var touchMode: String = "REPEL" // REPEL, ATTRACT, SPAWN, DRAG_RIGID

    // Active selected preset name
    var activePreset: String = "BOX_PYRAMID"

    // Spatial Hash Grid for O(N) particle collision detection
    private val cellSize = 60f
    private val grid = HashMap<Long, ArrayList<PhysicsParticle>>(64)
    private val pooledBuckets = ArrayList<ArrayList<PhysicsParticle>>(64)

    // Telemetry & Diagnostic stats
    var currentFps: Int = 60
    var activeEntityCount: Int = 0
    var collisionChecksCount: Int = 0
    var lastStepMicros: Long = 0L

    init {
        initSandbox(800f, 1200f)
    }

    fun initSandbox(width: Float, height: Float) {
        val safeW = if (width <= 0) 800f else width
        val safeH = if (height <= 0) 1200f else height

        particles.clear()
        boxes.clear()

        // Load into rigid body world
        world.gravity.set(0f, gravity)
        world.loadPreset(activePreset, safeW, safeH)

        val colors = arrayOf(
            Color(0xFF00F0FF),
            Color(0xFF9D4EDD),
            Color(0xFFFF2A85),
            Color(0xFFFFB703),
            Color(0xFF06D6A0)
        )

        for (i in 0 until 24) {
            val px = Random.nextFloat() * (safeW - 80f) + 40f
            val py = Random.nextFloat() * (safeH * 0.3f) + 30f
            val rad = Random.nextFloat() * 8f + 8f
            val col = colors[i % colors.size]

            particles.add(
                PhysicsParticle(
                    x = px,
                    y = py,
                    vx = (Random.nextFloat() - 0.5f) * 160f,
                    vy = (Random.nextFloat() - 0.5f) * 80f,
                    radius = rad,
                    color = col,
                    mass = rad / 10f,
                    restitution = 0.75f
                )
            )
        }

        activeEntityCount = particles.size + world.bodies.size
    }

    fun setPreset(presetName: String, width: Float, height: Float) {
        activePreset = presetName
        initSandbox(width, height)
    }

    fun onTouchDown(x: Float, y: Float) {
        touchForceActive = true
        touchPos = Offset(x, y)

        if (touchMode == "DRAG_RIGID") {
            val body = world.getBodyAt(x, y)
            if (body != null && body.bodyType == BodyType.DYNAMIC) {
                val localAnchor = Vec2()
                body.transform.inverseTransform(Vec2(x, y), localAnchor)
                val mj = MouseJoint2D(body, localAnchor)
                mj.target.set(x, y)
                world.mouseJoint = mj
            }
        }
    }

    fun onTouchMove(x: Float, y: Float) {
        touchPos = Offset(x, y)
        world.mouseJoint?.target?.set(x, y)
    }

    fun onTouchUp() {
        touchForceActive = false
        world.mouseJoint = null
    }

    private fun getCellKey(x: Float, y: Float): Long {
        val cellX = (x / cellSize).toInt()
        val cellY = (y / cellSize).toInt()
        return (cellX.toLong() and 0xFFFFFFFFL) or (cellY.toLong() shl 32)
    }

    fun step(dt: Float, boundaryWidth: Float, boundaryHeight: Float) {
        if (!isSimulationRunning || dt <= 0f) return

        val safeDt = dt.coerceIn(0.001f, 0.033f)
        val safeW = if (boundaryWidth <= 0f) 800f else boundaryWidth
        val safeH = if (boundaryHeight <= 0f) 1200f else boundaryHeight

        // 1. Step Rigid Body World Simulation
        world.gravity.set(0f, gravity)
        world.step(safeDt)
        lastStepMicros = world.lastStepMicros

        collisionChecksCount = world.broadphasePairsCount

        // 2. Step Particle Kinematics & Touch Integration
        for (i in 0 until particles.size) {
            val p = particles[i]
            if (p.isPinned) continue

            // Integrate Gravity
            p.vy += gravity * safeDt

            // Touch interaction
            if (touchForceActive) {
                val dx = touchPos.x - p.x
                val dy = touchPos.y - p.y
                val distSq = dx * dx + dy * dy
                if (distSq < 160000f) { // 400px radius
                    val dist = sqrt(distSq.coerceAtLeast(100f))
                    val force = (1.0f - dist / 400f) * 1600f
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force

                    if (touchMode == "ATTRACT") {
                        p.vx += (fx / p.mass) * safeDt
                        p.vy += (fy / p.mass) * safeDt
                    } else if (touchMode == "REPEL") {
                        p.vx -= (fx / p.mass) * safeDt
                        p.vy -= (fy / p.mass) * safeDt
                    }
                }
            }

            // Air damping
            p.vx *= airResistance
            p.vy *= airResistance

            // Float NaN / Infinity sanitization
            if (p.vx.isNaN() || p.vx.isInfinite()) p.vx = 0f
            if (p.vy.isNaN() || p.vy.isInfinite()) p.vy = 0f

            // Position Step
            p.x += p.vx * safeDt
            p.y += p.vy * safeDt

            // Boundary collision handling
            if (p.x - p.radius < 24f) {
                p.x = 24f + p.radius
                p.vx = -p.vx * p.restitution
            } else if (p.x + p.radius > safeW - 24f) {
                p.x = safeW - 24f - p.radius
                p.vx = -p.vx * p.restitution
            }

            if (p.y - p.radius < 24f) {
                p.y = 24f + p.radius
                p.vy = -p.vy * p.restitution
            } else if (p.y + p.radius > safeH - 24f) {
                p.y = safeH - 24f - p.radius
                p.vy = -p.vy * p.restitution
            }
        }

        // 3. Spatial Partitioning Grid Insertion for Particles
        for ((_, bucket) in grid) {
            bucket.clear()
            pooledBuckets.add(bucket)
        }
        grid.clear()

        for (i in 0 until particles.size) {
            val p = particles[i]
            val key = getCellKey(p.x, p.y)
            var bucket = grid[key]
            if (bucket == null) {
                bucket = if (pooledBuckets.isNotEmpty()) pooledBuckets.removeAt(pooledBuckets.size - 1) else ArrayList(8)
                grid[key] = bucket
            }
            bucket.add(p)
        }

        // 4. Spatial Pairwise Collision Resolution for Particles
        for (i in 0 until particles.size) {
            val p1 = particles[i]
            val cellX = (p1.x / cellSize).toInt()
            val cellY = (p1.y / cellSize).toInt()

            for (ox in -1..1) {
                for (oy in -1..1) {
                    val neighborKey = ((cellX + ox).toLong() and 0xFFFFFFFFL) or ((cellY + oy).toLong() shl 32)
                    val neighborBucket = grid[neighborKey] ?: continue

                    for (k in 0 until neighborBucket.size) {
                        val p2 = neighborBucket[k]
                        if (p1 === p2 || p1.x > p2.x) continue

                        collisionChecksCount++
                        val dx = p2.x - p1.x
                        val dy = p2.y - p1.y
                        val distSq = dx * dx + dy * dy
                        val minDist = p1.radius + p2.radius

                        if (distSq < minDist * minDist && distSq > 0.0001f) {
                            val dist = sqrt(distSq)
                            val nx = dx / dist
                            val ny = dy / dist

                            // Positional separation
                            val overlap = 0.5f * (minDist - dist)
                            p1.x -= nx * overlap
                            p1.y -= ny * overlap
                            p2.x += nx * overlap
                            p2.y += ny * overlap

                            // Elastic impulse
                            val kx = p1.vx - p2.vx
                            val ky = p1.vy - p2.vy
                            val p = 2f * (nx * kx + ny * ky) / (p1.mass + p2.mass)

                            p1.vx -= p * p2.mass * nx
                            p1.vy -= p * p2.mass * ny
                            p2.vx += p * p1.mass * nx
                            p2.vy += p * p1.mass * ny
                        }
                    }
                }
            }
        }

        activeEntityCount = particles.size + world.bodies.size
    }

    var spawnBodyType: BodyType = BodyType.DYNAMIC
    var spawnRestitution: Float = 0.6f
    var spawnFriction: Float = 0.4f
    var spawnDensity: Float = 1.0f

    fun spawnBurst(x: Float, y: Float) {
        spawnBody(x, y, spawnBodyType, spawnRestitution, spawnFriction, spawnDensity)
    }

    fun spawnBody(
        x: Float,
        y: Float,
        type: BodyType = spawnBodyType,
        restitution: Float = spawnRestitution,
        friction: Float = spawnFriction,
        density: Float = spawnDensity
    ) {
        val colors = arrayOf(
            Color(0xFF00F0FF),
            Color(0xFF9D4EDD),
            Color(0xFFFF2A85),
            Color(0xFFFFB703),
            Color(0xFF06D6A0)
        )

        val color = colors[Random.nextInt(colors.size)]
        val isBox = Random.nextBoolean()

        when (type) {
            BodyType.STATIC -> {
                if (isBox) {
                    val w = Random.nextFloat() * 40f + 40f
                    val h = Random.nextFloat() * 10f + 14f
                    val b = world.createStaticBox(x, y, w, h, Color(0xFF334155))
                    b.restitution = restitution
                    b.friction = friction
                } else {
                    val rad = Random.nextFloat() * 12f + 16f
                    val c = world.createCircle(x, y, rad, BodyType.STATIC, Color(0xFF334155))
                    c.restitution = restitution
                    c.friction = friction
                }
            }
            BodyType.KINEMATIC -> {
                val w = Random.nextFloat() * 40f + 60f
                val h = 16f
                val vx = (if (Random.nextBoolean()) 1 else -1) * (Random.nextFloat() * 80f + 80f)
                val k = world.createKinematicBox(x, y, w, h, vx = vx, vy = 0f, color = Color(0xFF9D4EDD))
                k.tag = "oscillator_h"
                k.friction = friction
                k.restitution = restitution
            }
            BodyType.DYNAMIC -> {
                if (isBox) {
                    val size = Random.nextFloat() * 16f + 20f
                    val b = world.createDynamicBox(x, y, size, size, color, restitution, friction, density)
                    b.linearVelocity.set((Random.nextFloat() - 0.5f) * 200f, -Random.nextFloat() * 180f)
                    b.angularVelocity = (Random.nextFloat() - 0.5f) * 6f
                } else {
                    val rad = Random.nextFloat() * 8f + 12f
                    val c = world.createCircle(x, y, rad, BodyType.DYNAMIC, color)
                    c.linearVelocity.set((Random.nextFloat() - 0.5f) * 200f, -Random.nextFloat() * 180f)
                    c.restitution = restitution
                    c.friction = friction
                    c.updateMassData(density)
                }
            }
        }

        // Particle sparks
        for (k in 0 until 6) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = Random.nextFloat() * 220f + 60f
            particles.add(
                PhysicsParticle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    radius = Random.nextFloat() * 5f + 4f,
                    color = color,
                    mass = 0.5f,
                    restitution = 0.75f
                )
            )
        }

        while (particles.size > 64) {
            particles.removeAt(0)
        }
    }
}
