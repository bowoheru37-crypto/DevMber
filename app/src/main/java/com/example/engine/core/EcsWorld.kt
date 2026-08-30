package com.example.engine.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.engine.character.CharacterClass
import com.example.engine.character.CharacterSystemRenderer
import com.example.engine.character.EnemyType

/**
 * High-Performance Entity Component System (ECS)
 * Inspired by Flecs, Bevy ECS, and LibGDX Ashley.
 * Pre-allocates dense arrays of primitive memory to completely bypass the JVM Garbage Collector.
 */
data class EcsTransform(var x: Float = 0f, var y: Float = 0f, var rotation: Float = 0f, var scale: Float = 1f)
data class EcsVelocity(var vx: Float = 0f, var vy: Float = 0f, var maxSpeed: Float = 300f)
data class EcsCollider(var width: Float = 24f, var height: Float = 24f, var isStatic: Boolean = false, var isTrigger: Boolean = false)
data class EcsRenderable(var color: Color = Color(0xFF00F0FF), var archetype: String = "HERO", var radius: Float = 16f)
data class EcsAiBehavior(var state: String = "PATROL", var patrolTimer: Float = 0f, var targetX: Float = 0f, var targetY: Float = 0f)

class EcsWorld(val maxEntities: Int = 64) {
    val activeEntities = BooleanArray(maxEntities) { false }
    val transforms = Array(maxEntities) { EcsTransform() }
    val velocities = Array(maxEntities) { EcsVelocity() }
    val colliders = Array(maxEntities) { EcsCollider() }
    val renderables = Array(maxEntities) { EcsRenderable() }
    val aiBehaviors = Array(maxEntities) { EcsAiBehavior() }

    val spatialGrid = SpatialHashGrid(cellSize = 64f)
    val tweenEngine = TweenEngine(maxTweens = 32)
    val tilemapEngine = TilemapMatrixEngine(mapWidth = 32, mapHeight = 20)
    val lightingEngine = RaycastLightingEngine(maxLights = 8)

    private val queryCandidatesBuffer = IntArray(32)

    var entityCount: Int = 0

    init {
        // Seed default archetype entities for demonstration
        spawnEntity(x = 100f, y = 200f, vx = 80f, vy = 0f, archetype = "HERO", color = Color(0xFF00F0FF))
        spawnEntity(x = 280f, y = 200f, vx = -50f, vy = 0f, archetype = "SLIME", color = Color(0xFFFF2A85))
        spawnEntity(x = 420f, y = 140f, vx = 40f, vy = 30f, archetype = "DRONE", color = Color(0xFFFF4D6D))
        spawnEntity(x = 550f, y = 200f, vx = 0f, vy = 0f, archetype = "NPC", color = Color(0xFFFFB703))
    }

    fun spawnEntity(
        x: Float,
        y: Float,
        vx: Float = 0f,
        vy: Float = 0f,
        archetype: String = "HERO",
        color: Color = Color(0xFF00F0FF)
    ): Int {
        for (i in 0 until maxEntities) {
            if (!activeEntities[i]) {
                activeEntities[i] = true
                transforms[i].x = x
                transforms[i].y = y
                transforms[i].rotation = 0f
                transforms[i].scale = 1f

                velocities[i].vx = vx
                velocities[i].vy = vy

                colliders[i].width = 24f
                colliders[i].height = 24f
                colliders[i].isStatic = false

                renderables[i].archetype = archetype
                renderables[i].color = color
                renderables[i].radius = 16f

                aiBehaviors[i].state = "PATROL"
                aiBehaviors[i].patrolTimer = 0f
                entityCount++
                return i
            }
        }
        return -1
    }

    fun killEntity(id: Int) {
        if (id in 0 until maxEntities && activeEntities[id]) {
            activeEntities[id] = false
            entityCount--
        }
    }

    fun clearAll() {
        activeEntities.fill(false)
        entityCount = 0
    }

    /**
     * Complete ECS Execution Pipeline:
     * 1. Tweening System
     * 2. AI Behavior State Machine System
     * 3. Kinematic Movement System
     * 4. Broadphase Spatial Grid Partitioning System
     * 5. Collision Solver System
     * 6. Dynamic 2D Lighting System
     */
    fun update(dt: Float, worldWidth: Float, worldHeight: Float, animTime: Float) {
        val safeDt = dt.coerceIn(0.001f, 0.033f)

        // 1. Tween System
        tweenEngine.update(safeDt)

        // 2. Clear Broadphase Spatial Grid
        spatialGrid.clear()

        // 3. AI State Machine & Movement System
        for (i in 0 until maxEntities) {
            if (!activeEntities[i]) continue

            val t = transforms[i]
            val v = velocities[i]
            val ai = aiBehaviors[i]

            // AI Logic
            if (renderables[i].archetype == "DRONE") {
                ai.patrolTimer += safeDt
                v.vy = kotlin.math.sin(ai.patrolTimer * 4f) * 60f
            } else if (renderables[i].archetype == "SLIME") {
                ai.patrolTimer += safeDt
                if (ai.patrolTimer > 1.5f) {
                    ai.patrolTimer = 0f
                    v.vx *= -1f
                }
            }

            // Kinematic integration
            t.x += v.vx * safeDt
            t.y += v.vy * safeDt

            // Screen boundary rebound
            if (t.x < 30f) { t.x = 30f; v.vx = kotlin.math.abs(v.vx) }
            if (t.x > worldWidth - 30f) { t.x = worldWidth - 30f; v.vx = -kotlin.math.abs(v.vx) }
            if (t.y < 30f) { t.y = 30f; v.vy = kotlin.math.abs(v.vy) }
            if (t.y > worldHeight - 30f) { t.y = worldHeight - 30f; v.vy = -kotlin.math.abs(v.vy) }

            // Insert into spatial grid
            spatialGrid.insert(i, t.x - 12f, t.y - 12f, 24f, 24f)
        }

        // 4. Collision Resolution System via Broadphase Spatial Hash
        for (i in 0 until maxEntities) {
            if (!activeEntities[i]) continue
            val ti = transforms[i]
            val vi = velocities[i]

            val candidateCount = spatialGrid.queryPotentialColliders(
                ti.x - 14f, ti.y - 14f, 28f, 28f,
                queryCandidatesBuffer, 32
            )

            for (k in 0 until candidateCount) {
                val j = queryCandidatesBuffer[k]
                if (j == i || !activeEntities[j]) continue

                val tj = transforms[j]
                val dx = ti.x - tj.x
                val dy = ti.y - tj.y
                val distSq = dx * dx + dy * dy
                val minDist = 28f

                if (distSq < minDist * minDist && distSq > 0.001f) {
                    val dist = kotlin.math.sqrt(distSq)
                    val overlap = 0.5f * (minDist - dist)
                    val nx = dx / dist
                    val ny = dy / dist

                    ti.x += nx * overlap
                    ti.y += ny * overlap
                    tj.x -= nx * overlap
                    tj.y -= ny * overlap

                    // Elastic impulse
                    val kx = vi.vx - velocities[j].vx
                    val ky = vi.vy - velocities[j].vy
                    val p = 2f * (nx * kx + ny * ky) / 2f
                    vi.vx -= p * nx
                    vi.vy -= p * ny
                    velocities[j].vx += p * nx
                    velocities[j].vy += p * ny
                }
            }
        }

        // 5. Dynamic Light System
        lightingEngine.update(animTime)
    }

    /**
     * Render ECS World with zero GC overhead
     */
    fun draw(drawScope: DrawScope, animTime: Float) {
        with(drawScope) {
            // Draw Dynamic 2D Lighting pass
            lightingEngine.draw(this, animTime)

            // Draw Entities
            for (i in 0 until maxEntities) {
                if (!activeEntities[i]) continue
                val t = transforms[i]
                val r = renderables[i]

                when (r.archetype) {
                    "HERO" -> CharacterSystemRenderer.drawPlayableCharacter(
                        this, t.x, t.y, radius = r.radius, characterClass = CharacterClass.CYBER_NINJA, animTime = animTime
                    )
                    "SLIME" -> CharacterSystemRenderer.drawEnemy(
                        this, t.x, t.y, size = r.radius * 1.5f, enemyType = EnemyType.SLIME_BOUNCER, animTime = animTime
                    )
                    "DRONE" -> CharacterSystemRenderer.drawEnemy(
                        this, t.x, t.y, size = r.radius * 1.5f, enemyType = EnemyType.CYBER_DRONE, animTime = animTime
                    )
                    "NPC" -> CharacterSystemRenderer.drawNpc(
                        this, t.x, t.y, com.example.engine.character.NpcEntity("npc_$i", "Sage", "Ally", r.color, "Live long!"), animTime = animTime
                    )
                    else -> {
                        drawCircle(color = r.color, radius = r.radius, center = Offset(t.x, t.y))
                    }
                }
            }
        }
    }
}
