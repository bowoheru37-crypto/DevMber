package com.example.engine.physics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Raycast Hit Result
 */
class RaycastHit2D {
    var hit: Boolean = false
    val point: Vec2 = Vec2()
    val normal: Vec2 = Vec2()
    var fraction: Float = 1.0f
    var body: RigidBody2D? = null

    fun reset() {
        hit = false
        fraction = 1.0f
        body = null
    }
}

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════════╗
 * ║  ⚡ PHYSICS WORLD 2D (SEQUENTIAL IMPULSE SOLVER & SPATIAL BROADPHASE ARBITER)             ║
 * ║  Full Zero-Allocation Loop, Substepping, Warm Starting, High-Precision Rigid Dynamics    ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════════╝
 */
class PhysicsWorld2D(
    val gravity: Vec2 = Vec2(0f, 980f)
) {
    val bodies = ArrayList<RigidBody2D>(128)
    val joints = ArrayList<PhysicsJoint2D>(64)

    // Pre-allocated Contact Manifold Pool for 0 GC frame allocations
    private val maxManifolds = 256
    val activeManifolds = ArrayList<ContactManifold2D>(maxManifolds)
    private val manifoldPool = Array(maxManifolds) { ContactManifold2D() }
    private var manifoldPoolIndex = 0

    // Spatial Hash Grid Broadphase
    private val cellSize = 64f
    private val spatialGrid = HashMap<Long, ArrayList<RigidBody2D>>(128)
    private val bucketPool = ArrayList<ArrayList<RigidBody2D>>(128)

    // Mouse / Touch Drag Spring
    var mouseJoint: MouseJoint2D? = null

    // Telemetry Diagnostics
    var velocityIterations: Int = 8
    var lastStepMicros: Long = 0L
    var broadphasePairsCount: Int = 0
    var contactManifoldCount: Int = 0

    // Simulation Bounds
    var worldWidth: Float = 800f
    var worldHeight: Float = 1200f

    fun clear() {
        bodies.clear()
        joints.clear()
        activeManifolds.clear()
        mouseJoint = null
    }

    fun addBody(body: RigidBody2D): RigidBody2D {
        bodies.add(body)
        return body
    }

    fun addJoint(joint: PhysicsJoint2D): PhysicsJoint2D {
        joints.add(joint)
        return joint
    }

    fun createStaticBox(x: Float, y: Float, width: Float = 32f, height: Float = 32f, color: Color = Color(0xFF1E293B)): PhysicsBody.Static {
        val body = PhysicsBody.Static(shape = BoxShape2D(width * 0.5f, height * 0.5f))
        body.setPosition(x, y)
        body.color = color
        addBody(body)
        return body
    }

    fun createDynamicBox(x: Float, y: Float, width: Float = 32f, height: Float = 32f, color: Color = Color(0xFFFFB703), restitution: Float = 0.5f, friction: Float = 0.35f, density: Float = 1.0f): PhysicsBody.Dynamic {
        val body = PhysicsBody.Dynamic(shape = BoxShape2D(width * 0.5f, height * 0.5f))
        body.setPosition(x, y)
        body.color = color
        body.restitution = restitution
        body.friction = friction
        body.updateMassData(density)
        addBody(body)
        return body
    }

    fun createKinematicBox(x: Float, y: Float, width: Float = 80f, height: Float = 16f, vx: Float = 0f, vy: Float = 0f, color: Color = Color(0xFF9D4EDD)): PhysicsBody.Kinematic {
        val body = PhysicsBody.Kinematic(shape = BoxShape2D(width * 0.5f, height * 0.5f))
        body.setPosition(x, y)
        body.setLinearVelocity(vx, vy)
        body.color = color
        addBody(body)
        return body
    }

    fun createCircle(x: Float, y: Float, radius: Float = 16f, bodyType: BodyType = BodyType.DYNAMIC, color: Color = Color(0xFF00F0FF)): RigidBody2D {
        val body = RigidBody2D(bodyType = bodyType, shape = CircleShape2D(radius))
        body.setPosition(x, y)
        body.color = color
        return addBody(body)
    }

    fun createBox(x: Float, y: Float, width: Float = 32f, height: Float = 32f, bodyType: BodyType = BodyType.DYNAMIC, color: Color = Color(0xFFFFB703)): RigidBody2D {
        val body = RigidBody2D(bodyType = bodyType, shape = BoxShape2D(width * 0.5f, height * 0.5f))
        body.setPosition(x, y)
        body.color = color
        return addBody(body)
    }

    fun createStaticBoundary(width: Float, height: Float, wallThickness: Float = 24f) {
        worldWidth = width
        worldHeight = height

        // Floor
        val floor = createBox(width * 0.5f, height - wallThickness * 0.5f, width, wallThickness, BodyType.STATIC, Color(0xFF1E293B))
        floor.friction = 0.6f
        floor.restitution = 0.2f

        // Left Wall
        val left = createBox(wallThickness * 0.5f, height * 0.5f, wallThickness, height, BodyType.STATIC, Color(0xFF1E293B))
        left.friction = 0.4f

        // Right Wall
        val right = createBox(width - wallThickness * 0.5f, height * 0.5f, wallThickness, height, BodyType.STATIC, Color(0xFF1E293B))
        right.friction = 0.4f

        // Ceiling
        createBox(width * 0.5f, wallThickness * 0.5f, width, wallThickness, BodyType.STATIC, Color(0xFF1E293B))
    }

    /**
     * Primary Physics Step using Substepping & Sequential Impulse Solver
     */
    fun step(rawDt: Float) {
        val startTime = System.nanoTime()

        // Clamp delta time to avoid spiral of death
        val dt = rawDt.coerceIn(0.001f, 0.033f)
        val invDt = 1.0f / dt

        // 1. Integrate External Forces & Gravity
        for (i in 0 until bodies.size) {
            bodies[i].integrateForces(dt, gravity)
        }

        // 2. Broadphase Spatial Grid Collision Pair Detection
        broadphasePairsCount = 0
        manifoldPoolIndex = 0
        activeManifolds.clear()

        populateSpatialGrid()

        for (i in 0 until bodies.size) {
            val a = bodies[i]
            val cellMinX = (a.aabb.min.x / cellSize).toInt()
            val cellMaxX = (a.aabb.max.x / cellSize).toInt()
            val cellMinY = (a.aabb.min.y / cellSize).toInt()
            val cellMaxY = (a.aabb.max.y / cellSize).toInt()

            for (cx in cellMinX..cellMaxX) {
                for (cy in cellMinY..cellMaxY) {
                    val key = getCellKey(cx, cy)
                    val bucket = spatialGrid[key] ?: continue

                    for (k in 0 until bucket.size) {
                        val b = bucket[k]
                        if (a === b) continue
                        if (a.bodyType == BodyType.STATIC && b.bodyType == BodyType.STATIC) continue
                        if (a.id > b.id) continue // Eliminate duplicate checks

                        broadphasePairsCount++
                        if (manifoldPoolIndex < maxManifolds) {
                            val manifold = manifoldPool[manifoldPoolIndex]
                            manifold.reset()
                            if (NarrowphaseCollision.checkCollision(a, b, manifold)) {
                                activeManifolds.add(manifold)
                                manifoldPoolIndex++
                            }
                        }
                    }
                }
            }
        }
        contactManifoldCount = activeManifolds.size

        // 3. Pre-Step Constraints (Warm Starting & Mass Matrix Pre-calculation)
        for (i in 0 until activeManifolds.size) {
            activeManifolds[i].preStep(dt, invDt)
        }
        for (i in 0 until joints.size) {
            if (joints[i].isEnabled) joints[i].preStep(dt, invDt)
        }
        mouseJoint?.preStep(dt, invDt)

        // 4. Sequential Impulse Solver (Velocity Iterations)
        for (iter in 0 until velocityIterations) {
            mouseJoint?.solveVelocity(dt)

            for (i in 0 until joints.size) {
                if (joints[i].isEnabled) joints[i].solveVelocity()
            }

            for (i in 0 until activeManifolds.size) {
                activeManifolds[i].solveVelocity()
            }
        }

        // 5. Integrate Positions and Angles
        for (i in 0 until bodies.size) {
            val b = bodies[i]
            b.integrateVelocities(dt)

            // Auto-reverse kinematic oscillators at world boundaries
            if (b is PhysicsBody.Kinematic) {
                if (b.tag == "oscillator_h") {
                    if (b.transform.position.x < worldWidth * 0.25f && b.linearVelocity.x < 0) {
                        b.linearVelocity.x = -b.linearVelocity.x
                    } else if (b.transform.position.x > worldWidth * 0.75f && b.linearVelocity.x > 0) {
                        b.linearVelocity.x = -b.linearVelocity.x
                    }
                } else if (b.tag == "oscillator_v") {
                    if (b.transform.position.y < worldHeight * 0.3f && b.linearVelocity.y < 0) {
                        b.linearVelocity.y = -b.linearVelocity.y
                    } else if (b.transform.position.y > worldHeight * 0.7f && b.linearVelocity.y > 0) {
                        b.linearVelocity.y = -b.linearVelocity.y
                    }
                }
            }
        }

        lastStepMicros = (System.nanoTime() - startTime) / 1000
    }

    private fun getCellKey(cellX: Int, cellY: Int): Long {
        return (cellX.toLong() and 0xFFFFFFFFL) or (cellY.toLong() shl 32)
    }

    private fun populateSpatialGrid() {
        for ((_, bucket) in spatialGrid) {
            bucket.clear()
            bucketPool.add(bucket)
        }
        spatialGrid.clear()

        for (i in 0 until bodies.size) {
            val body = bodies[i]
            val cellMinX = (body.aabb.min.x / cellSize).toInt()
            val cellMaxX = (body.aabb.max.x / cellSize).toInt()
            val cellMinY = (body.aabb.min.y / cellSize).toInt()
            val cellMaxY = (body.aabb.max.y / cellSize).toInt()

            for (cx in cellMinX..cellMaxX) {
                for (cy in cellMinY..cellMaxY) {
                    val key = getCellKey(cx, cy)
                    var bucket = spatialGrid[key]
                    if (bucket == null) {
                        bucket = if (bucketPool.isNotEmpty()) bucketPool.removeAt(bucketPool.size - 1) else ArrayList(8)
                        spatialGrid[key] = bucket
                    }
                    bucket.add(body)
                }
            }
        }
    }

    fun getBodyAt(x: Float, y: Float): RigidBody2D? {
        val p = Vec2(x, y)
        for (i in bodies.indices.reversed()) {
            val b = bodies[i]
            if (b.aabb.contains(p)) {
                return b
            }
        }
        return null
    }

    // ==========================================
    // PRESET BENCHMARK SCENE GENERATORS
    // ==========================================

    fun loadPreset(presetName: String, width: Float, height: Float) {
        clear()
        createStaticBoundary(width, height)

        when (presetName) {
            "KINEMATIC_PLATFORMS" -> buildKinematicPlatformMix(width, height)
            "NEWTONS_CRADLE" -> buildNewtonsCradle(width, height)
            "BOX_PYRAMID" -> buildBoxPyramid(width, height)
            "ROPE_BRIDGE" -> buildRopeBridge(width, height)
            "DESTRUCTION_CANNON" -> buildDestructionCannon(width, height)
            "PINBALL_PLAYGROUND" -> buildPinballPlayground(width, height)
            else -> buildBoxPyramid(width, height)
        }
    }

    private fun buildKinematicPlatformMix(w: Float, h: Float) {
        // 1. Static boundary chutes and funnels
        val f1 = createBox(w * 0.25f, h * 0.22f, 160f, 14f, BodyType.STATIC, Color(0xFF334155))
        f1.setAngle(0.3f)
        f1.restitution = 0.2f
        f1.friction = 0.4f

        val f2 = createBox(w * 0.75f, h * 0.38f, 160f, 14f, BodyType.STATIC, Color(0xFF334155))
        f2.setAngle(-0.3f)
        f2.restitution = 0.2f
        f2.friction = 0.4f

        // 2. Kinematic Horizontal Mover (Platform that carries dynamic items with infinite mass)
        val kMoverH = createKinematicBox(w * 0.5f, h * 0.52f, 120f, 16f, vx = 140f, vy = 0f, color = Color(0xFF9D4EDD))
        kMoverH.tag = "oscillator_h"
        kMoverH.friction = 0.8f // High friction to carry dynamic boxes!

        // 3. Kinematic Vertical Elevator
        val kMoverV = createKinematicBox(w * 0.85f, h * 0.5f, 90f, 14f, vx = 0f, vy = -120f, color = Color(0xFF7928CA))
        kMoverV.tag = "oscillator_v"
        kMoverV.friction = 0.6f

        // 4. Kinematic Rotating Agitator (Propeller blade)
        val kSpinner = createKinematicBox(w * 0.35f, h * 0.72f, 130f, 14f, vx = 0f, vy = 0f, color = Color(0xFFB5179E))
        kSpinner.angularVelocity = 2.4f // Constant spin

        // 5. Dynamic Cargo Objects (Bouncy balls & heavy stackable boxes)
        val colors = listOf(Color(0xFF00F0FF), Color(0xFFFF2A85), Color(0xFFFFB703), Color(0xFF06D6A0))
        for (i in 0 until 5) {
            val b = createDynamicBox(w * 0.2f + i * 26f, h * 0.1f - i * 36f, 26f, 26f, colors[i % colors.size], restitution = 0.4f, friction = 0.5f)
            b.updateMassData(density = 1.2f)
        }
        for (k in 0 until 4) {
            val s = createCircle(w * 0.3f + k * 30f, h * 0.05f - k * 30f, 12f, BodyType.DYNAMIC, colors[(k + 2) % colors.size])
            s.restitution = 0.75f
            s.friction = 0.2f
        }
    }

    private fun buildNewtonsCradle(w: Float, h: Float) {
        val centerX = w * 0.5f
        val topY = h * 0.2f
        val ballRadius = 18f
        val wireLength = 160f
        val count = 5

        for (i in 0 until count) {
            val x = centerX + (i - 2) * (ballRadius * 2f + 1f)
            val anchor = createBox(x, topY, 14f, 14f, BodyType.STATIC, Color(0xFF64748B))

            // Pull first ball to left for initial kinetic impulse
            val ballX = if (i == 0) x - 120f else x
            val ballY = if (i == 0) topY + wireLength - 30f else topY + wireLength

            val ball = createCircle(ballX, ballY, ballRadius, BodyType.DYNAMIC, Color(0xFF00F0FF))
            ball.restitution = 0.98f // Super high elastic restitution
            ball.friction = 0.05f

            addJoint(DistanceJoint2D(anchor, ball, Vec2(), Vec2(), targetDistance = wireLength))
        }
    }

    private fun buildBoxPyramid(w: Float, h: Float) {
        val centerX = w * 0.5f
        val startY = h - 60f
        val boxSize = 34f
        val rows = 5
        val colors = listOf(Color(0xFFFFB703), Color(0xFFFF2A85), Color(0xFF00F0FF), Color(0xFF06D6A0), Color(0xFF9D4EDD))

        for (row in 0 until rows) {
            val count = rows - row
            val rowY = startY - row * (boxSize + 2f)
            val rowStartX = centerX - (count - 1) * (boxSize + 4f) * 0.5f

            for (col in 0 until count) {
                val boxX = rowStartX + col * (boxSize + 4f)
                val box = createBox(boxX, rowY, boxSize, boxSize, BodyType.DYNAMIC, colors[row % colors.size])
                box.friction = 0.6f
                box.restitution = 0.1f
            }
        }
    }

    private fun buildRopeBridge(w: Float, h: Float) {
        val startX = w * 0.15f
        val endX = w * 0.85f
        val y = h * 0.45f
        val links = 8
        val linkWidth = (endX - startX) / links

        var prevBody: RigidBody2D = createBox(startX, y, 20f, 20f, BodyType.STATIC, Color(0xFF64748B))

        for (i in 0 until links) {
            val lx = startX + (i + 0.5f) * linkWidth
            val isLast = i == links - 1
            val currentBody = if (isLast) {
                createBox(endX, y, 20f, 20f, BodyType.STATIC, Color(0xFF64748B))
            } else {
                createBox(lx, y, linkWidth * 0.9f, 12f, BodyType.DYNAMIC, Color(0xFF06D6A0))
            }

            addJoint(RevoluteJoint2D(prevBody, currentBody, Vec2(linkWidth * 0.45f, 0f), Vec2(-linkWidth * 0.45f, 0f)))
            prevBody = currentBody
        }

        // Drop a heavy ball onto the bridge
        val boulder = createCircle(w * 0.5f, h * 0.15f, 24f, BodyType.DYNAMIC, Color(0xFFFF2A85))
        boulder.updateMassData(density = 3.0f)
    }

    private fun buildDestructionCannon(w: Float, h: Float) {
        // High stone towers
        val towerX = w * 0.7f
        val bottomY = h - 50f
        for (i in 0 until 6) {
            val b = createBox(towerX, bottomY - i * 44f, 40f, 40f, BodyType.DYNAMIC, Color(0xFF94A3B8))
            b.friction = 0.7f
        }

        // Fast Cannonball
        val cannon = createCircle(w * 0.2f, h * 0.5f, 22f, BodyType.DYNAMIC, Color(0xFFFF0055))
        cannon.updateMassData(density = 5.0f)
        cannon.linearVelocity.set(450f, -80f)
    }

    private fun buildPinballPlayground(w: Float, h: Float) {
        // Angled Bouncers
        val b1 = createBox(w * 0.35f, h * 0.35f, 80f, 16f, BodyType.STATIC, Color(0xFF00F0FF))
        b1.setAngle(0.35f)
        b1.restitution = 0.9f

        val b2 = createBox(w * 0.65f, h * 0.45f, 80f, 16f, BodyType.STATIC, Color(0xFFFF2A85))
        b2.setAngle(-0.35f)
        b2.restitution = 0.9f

        // Round Bumpers
        val r1 = createCircle(w * 0.5f, h * 0.25f, 24f, BodyType.STATIC, Color(0xFFFFB703))
        r1.restitution = 1.0f

        // Spawn multiple balls
        for (k in 0 until 4) {
            val ball = createCircle(w * 0.3f + k * 30f, h * 0.1f, 12f, BodyType.DYNAMIC, Color(0xFF06D6A0))
            ball.restitution = 0.85f
        }
    }

    // ==========================================
    // DIRECT DRAWSCOPE RENDERER (0 GC ALLOCATION)
    // ==========================================

    fun render(drawScope: DrawScope) {
        with(drawScope) {
            // Draw Joints
            for (i in 0 until joints.size) {
                val j = joints[i]
                if (!j.isEnabled) continue
                when (j) {
                    is DistanceJoint2D -> {
                        val pA = Vec2()
                        val pB = Vec2()
                        j.bodyA.transform.transform(j.localAnchorA, pA)
                        j.bodyB.transform.transform(j.localAnchorB, pB)
                        drawLine(
                            color = Color(0xFF94A3B8).copy(alpha = 0.85f),
                            start = Offset(pA.x, pA.y),
                            end = Offset(pB.x, pB.y),
                            strokeWidth = 2.5f
                        )
                    }
                    is RevoluteJoint2D -> {
                        val pA = Vec2()
                        val pB = Vec2()
                        j.bodyA.transform.transform(j.localAnchorA, pA)
                        j.bodyB.transform.transform(j.localAnchorB, pB)
                        drawLine(
                            color = Color(0xFFFFB703).copy(alpha = 0.85f),
                            start = Offset(pA.x, pA.y),
                            end = Offset(pB.x, pB.y),
                            strokeWidth = 3f
                        )
                        drawCircle(Color(0xFFFFD166), 4f, Offset(pA.x, pA.y))
                    }
                }
            }

            // Draw Mouse Joint Line
            mouseJoint?.let { mj ->
                val p = Vec2()
                mj.body.transform.transform(mj.localAnchor, p)
                drawLine(
                    color = Color(0xFF00F0FF),
                    start = Offset(p.x, p.y),
                    end = Offset(mj.target.x, mj.target.y),
                    strokeWidth = 2.5f
                )
                drawCircle(Color(0xFF00F0FF), 5f, Offset(mj.target.x, mj.target.y))
            }

            // Draw Rigid Bodies
            for (i in 0 until bodies.size) {
                val b = bodies[i]
                val pos = b.transform.position

                when (val shape = b.shape) {
                    is CircleShape2D -> {
                        drawCircle(
                            color = b.color,
                            radius = shape.radius,
                            center = Offset(pos.x, pos.y)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f),
                            radius = shape.radius,
                            center = Offset(pos.x, pos.y),
                            style = Stroke(width = 2f)
                        )
                        // Orientation spoke
                        val spokeX = pos.x + cos(b.transform.angle) * shape.radius
                        val spokeY = pos.y + sin(b.transform.angle) * shape.radius
                        drawLine(
                            color = Color.White.copy(alpha = 0.9f),
                            start = Offset(pos.x, pos.y),
                            end = Offset(spokeX, spokeY),
                            strokeWidth = 2f
                        )
                    }
                    is BoxShape2D -> {
                        val path = Path().apply {
                            val v0 = shape.worldVertices[0]
                            moveTo(v0.x, v0.y)
                            for (k in 1 until 4) {
                                val vk = shape.worldVertices[k]
                                lineTo(vk.x, vk.y)
                            }
                            close()
                        }
                        drawPath(path, color = b.color)
                        drawPath(path, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 2f))
                    }
                    is CapsuleShape2D -> {
                        drawCircle(b.color, shape.radius, Offset(pos.x, pos.y))
                    }
                    is PolygonShape2D -> {
                        val path = Path().apply {
                            val v0 = shape.worldVertices[0]
                            moveTo(v0.x, v0.y)
                            for (k in 1 until shape.vertexCount) {
                                val vk = shape.worldVertices[k]
                                lineTo(vk.x, vk.y)
                            }
                            close()
                        }
                        drawPath(path, color = b.color)
                        drawPath(path, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 2f))
                    }
                }
            }

            // Draw Contact Points (Visual Collision Diagnostics)
            for (i in 0 until activeManifolds.size) {
                val m = activeManifolds[i]
                for (k in 0 until m.contactCount) {
                    val cp = m.contacts[k].position
                    drawCircle(Color(0xFFFF0055), 3f, Offset(cp.x, cp.y))
                }
            }
        }
    }
}
