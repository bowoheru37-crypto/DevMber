package com.example.engine.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Character Archetype Profiles
 */
enum class CharacterClass(
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val skillName: String,
    val baseHp: Int,
    val baseSpeed: Float,
    val description: String
) {
    CYBER_NINJA(
        title = "Cyber Ninja",
        primaryColor = Color(0xFF00F0FF),
        secondaryColor = Color(0xFF00363D),
        skillName = "Phase Shift Dash",
        baseHp = 3,
        baseSpeed = 220f,
        description = "High agility with extended invulnerability dash frames and double jump trail."
    ),
    SOLAR_KNIGHT(
        title = "Solar Knight",
        primaryColor = Color(0xFFFFB703),
        secondaryColor = Color(0xFF5E3A00),
        skillName = "Radiant Aegis",
        baseHp = 5,
        baseSpeed = 170f,
        description = "Fortified guardian with energy barrier and magnetic item attraction."
    ),
    VOID_WARLOCK(
        title = "Void Warlock",
        primaryColor = Color(0xFF9D4EDD),
        secondaryColor = Color(0xFF240046),
        skillName = "Singularity Pulse",
        baseHp = 3,
        baseSpeed = 190f,
        description = "Gravity manipulator with antigravity levitation and crystal charge burst."
    ),
    MECHA_TITAN(
        title = "Mecha Titan",
        primaryColor = Color(0xFF06D6A0),
        secondaryColor = Color(0xFF004D40),
        skillName = "Quake Slam",
        baseHp = 6,
        baseSpeed = 150f,
        description = "Heavy vanguard that smashes obstacles and converts impact into kinetic shield."
    )
}

/**
 * Enemy Archetypes with distinct AI Behaviors
 */
enum class EnemyType(
    val title: String,
    val color: Color,
    val aiBehavior: String,
    val damage: Int,
    val isFlying: Boolean
) {
    SLIME_BOUNCER("Slime Bouncer", Color(0xFFFF2A85), "Squish & Hop Patrol", 1, false),
    CYBER_DRONE("Cyber Drone", Color(0xFFFF4D6D), "Sine-Wave Hover & Dive", 1, true),
    VOID_STALKER("Void Stalker", Color(0xFF7209B7), "Phase Teleport Ambush", 2, true),
    MECHA_BOSS("Titan Golem", Color(0xFFE63946), "Heavy Ground Stomp & Shockwave", 3, false)
}

/**
 * NPC (Non-Player Character) Dialogue and Quest Entity
 */
data class NpcEntity(
    val id: String,
    val name: String,
    val role: String,
    val color: Color,
    val dialogue: String,
    val questRewardScore: Int = 500,
    var hasInteracted: Boolean = false
)

/**
 * High-Performance Character, Enemy, and NPC Vector Render System.
 * 100% zero image allocation, pure mathematical geometry rendering.
 */
object CharacterSystemRenderer {

    /**
     * Render Playable Character with Class Identity, Visor, Armor, and Animation States
     */
    fun drawPlayableCharacter(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        radius: Float = 20f,
        characterClass: CharacterClass = CharacterClass.CYBER_NINJA,
        isJumping: Boolean = false,
        isDashing: Boolean = false,
        animTime: Float = 0f,
        shieldActive: Boolean = false
    ) {
        with(drawScope) {
            val bounce = if (isJumping) -5f else (sin(animTime * 12f) * 2.5f)
            val center = Offset(x, y + bounce)

            // 1. Dash After-Image Trails
            if (isDashing) {
                drawCircle(
                    color = characterClass.primaryColor.copy(alpha = 0.4f),
                    radius = radius * 1.4f,
                    center = Offset(x - 22f, y + bounce)
                )
                drawCircle(
                    color = characterClass.primaryColor.copy(alpha = 0.2f),
                    radius = radius * 1.7f,
                    center = Offset(x - 44f, y + bounce)
                )
            }

            // 2. Class Shield Aura (Solar Knight or Power-Up)
            if (shieldActive || characterClass == CharacterClass.SOLAR_KNIGHT) {
                val pulse = (sin(animTime * 6f) + 1f) * 0.5f
                drawCircle(
                    color = characterClass.primaryColor.copy(alpha = 0.25f + pulse * 0.15f),
                    radius = radius * 1.35f + pulse * 3f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }

            // 3. Main Character Body & Armor
            drawCircle(
                color = characterClass.primaryColor,
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = radius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // 4. Class-Specific Helmets & Accents
            when (characterClass) {
                CharacterClass.CYBER_NINJA -> {
                    // Cyber Visor Horizon Line
                    val visorPath = Path().apply {
                        moveTo(center.x - radius * 0.7f, center.y - 2f)
                        lineTo(center.x + radius * 0.7f, center.y - 2f)
                        lineTo(center.x + radius * 0.4f, center.y + 4f)
                        lineTo(center.x - radius * 0.4f, center.y + 4f)
                        close()
                    }
                    drawPath(visorPath, color = Color(0xFF00363D))
                    drawCircle(color = Color(0xFF00F0FF), radius = 2.5f, center = Offset(center.x + 3f, center.y))
                }
                CharacterClass.SOLAR_KNIGHT -> {
                    // Golden Knight Crest
                    val crestPath = Path().apply {
                        moveTo(center.x, center.y - radius * 1.5f)
                        lineTo(center.x + 6f, center.y - radius * 0.7f)
                        lineTo(center.x - 6f, center.y - radius * 0.7f)
                        close()
                    }
                    drawPath(crestPath, color = Color(0xFFFFE169))
                    drawPath(crestPath, color = Color.White, style = Stroke(width = 1f))
                }
                CharacterClass.VOID_WARLOCK -> {
                    // Void Horns / Dark Magic Crown
                    val hornLeft = Path().apply {
                        moveTo(center.x - radius * 0.6f, center.y - radius * 0.6f)
                        lineTo(center.x - radius * 0.9f, center.y - radius * 1.3f)
                        lineTo(center.x - radius * 0.3f, center.y - radius * 0.9f)
                        close()
                    }
                    drawPath(hornLeft, color = Color(0xFFE0AAFF))
                    val hornRight = Path().apply {
                        moveTo(center.x + radius * 0.6f, center.y - radius * 0.6f)
                        lineTo(center.x + radius * 0.9f, center.y - radius * 1.3f)
                        lineTo(center.x + radius * 0.3f, center.y - radius * 0.9f)
                        close()
                    }
                    drawPath(hornRight, color = Color(0xFFE0AAFF))
                }
                CharacterClass.MECHA_TITAN -> {
                    // Reinforced Mecha Plating
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x - radius * 0.7f, center.y),
                        end = Offset(center.x + radius * 0.7f, center.y),
                        strokeWidth = 3f
                    )
                }
            }

            // 5. Jump Flame Propulsion Jet
            if (isJumping) {
                val flameFlicker = sin(animTime * 30f) * 4f
                val flamePath = Path().apply {
                    moveTo(center.x - radius * 0.45f, center.y + radius * 0.8f)
                    lineTo(center.x, center.y + radius * 1.7f + flameFlicker)
                    lineTo(center.x + radius * 0.45f, center.y + radius * 0.8f)
                    close()
                }
                drawPath(flamePath, color = Color(0xFFFFB703))
                drawPath(flamePath, color = Color(0xFFFF2A85), style = Stroke(width = 1.5f))
            }
        }
    }

    /**
     * Render Dynamic Enemy by Type (Slime, Drone, Void Stalker, Mecha Boss)
     */
    fun drawEnemy(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        size: Float = 24f,
        enemyType: EnemyType = EnemyType.SLIME_BOUNCER,
        animTime: Float = 0f
    ) {
        with(drawScope) {
            when (enemyType) {
                EnemyType.SLIME_BOUNCER -> {
                    val squish = sin(animTime * 8f) * 3f
                    val w = size * 0.7f + squish
                    val h = size * 0.7f - squish
                    drawOval(color = enemyType.color, topLeft = Offset(x - w, y - h), size = Size(w * 2f, h * 2f))
                    drawOval(color = Color(0xFFFF70A6), topLeft = Offset(x - w * 0.7f, y - h * 0.7f), size = Size(w * 1.4f, h * 1.4f))
                    drawOval(color = Color.White, topLeft = Offset(x - w, y - h), size = Size(w * 2f, h * 2f), style = Stroke(width = 1.5f))

                    // Angry Eyes
                    drawCircle(color = Color.White, radius = 4f, center = Offset(x - 5f, y - 2f))
                    drawCircle(color = Color(0xFF590D22), radius = 2f, center = Offset(x - 4f, y - 2f))
                    drawCircle(color = Color.White, radius = 4f, center = Offset(x + 5f, y - 2f))
                    drawCircle(color = Color(0xFF590D22), radius = 2f, center = Offset(x + 6f, y - 2f))
                }
                EnemyType.CYBER_DRONE -> {
                    val hover = sin(animTime * 10f) * 6f
                    val cy = y + hover
                    val r = size * 0.6f

                    // Rotor Blades
                    val bladeW = r * 1.8f
                    drawLine(color = Color(0xFF00F0FF), start = Offset(x - bladeW, cy - r), end = Offset(x + bladeW, cy - r), strokeWidth = 2f)

                    // Drone Eye Sphere
                    drawCircle(color = enemyType.color, radius = r, center = Offset(x, cy))
                    drawCircle(color = Color.White, radius = r, center = Offset(x, cy), style = Stroke(width = 2f))
                    drawCircle(color = Color(0xFFFF0055), radius = r * 0.4f, center = Offset(x + 2f, cy))
                }
                EnemyType.VOID_STALKER -> {
                    val pulse = sin(animTime * 6f) * 4f
                    val r = size * 0.75f + pulse
                    drawCircle(color = enemyType.color.copy(alpha = 0.3f), radius = r * 1.3f, center = Offset(x, y))
                    drawCircle(color = enemyType.color, radius = r, center = Offset(x, y))
                    drawCircle(color = Color.White, radius = r, center = Offset(x, y), style = Stroke(width = 2f))

                    // 3-Point Void Eye
                    drawCircle(color = Color(0xFFE0AAFF), radius = 4f, center = Offset(x - 4f, y - 2f))
                    drawCircle(color = Color(0xFFE0AAFF), radius = 4f, center = Offset(x + 4f, y - 2f))
                    drawCircle(color = Color(0xFFE0AAFF), radius = 4f, center = Offset(x, y + 5f))
                }
                EnemyType.MECHA_BOSS -> {
                    val s = size * 1.2f
                    // Heavy Armor Octagon
                    val bossPath = Path().apply {
                        moveTo(x - s * 0.6f, y - s)
                        lineTo(x + s * 0.6f, y - s)
                        lineTo(x + s, y - s * 0.4f)
                        lineTo(x + s, y + s * 0.4f)
                        lineTo(x + s * 0.6f, y + s)
                        lineTo(x - s * 0.6f, y + s)
                        lineTo(x - s, y + s * 0.4f)
                        lineTo(x - s, y - s * 0.4f)
                        close()
                    }
                    drawPath(bossPath, color = enemyType.color)
                    drawPath(bossPath, color = Color.White, style = Stroke(width = 2.5f))
                    drawCircle(color = Color(0xFFFFD166), radius = s * 0.35f, center = Offset(x, y))
                }
            }
        }
    }

    /**
     * Render Friendly NPC Entity with Speech Indicator and Quest Marker
     */
    fun drawNpc(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        npc: NpcEntity,
        animTime: Float = 0f
    ) {
        with(drawScope) {
            val floatOffset = sin(animTime * 4f) * 3f
            val cy = y + floatOffset
            val r = 22f

            // NPC Halo / Aura
            drawCircle(
                color = npc.color.copy(alpha = 0.25f),
                radius = r * 1.4f,
                center = Offset(x, cy)
            )

            // NPC Body
            drawCircle(
                color = npc.color,
                radius = r,
                center = Offset(x, cy)
            )
            drawCircle(
                color = Color.White,
                radius = r,
                center = Offset(x, cy),
                style = Stroke(width = 2f)
            )

            // Friendly Smiling Eyes
            drawCircle(color = Color.White, radius = 3.5f, center = Offset(x - 5f, cy - 3f))
            drawCircle(color = Color.White, radius = 3.5f, center = Offset(x + 5f, cy - 3f))

            // Quest Indicator Floating Icon (Exclamation mark / Star)
            val questPulse = (sin(animTime * 8f) + 1f) * 3f
            val questCenter = Offset(x, cy - r * 1.6f - questPulse)
            drawCircle(color = Color(0xFFFFB703), radius = 8f, center = questCenter)
            drawCircle(color = Color.White, radius = 8f, center = questCenter, style = Stroke(width = 1.5f))
            drawLine(
                color = Color(0xFF00363D),
                start = Offset(questCenter.x, questCenter.y - 4f),
                end = Offset(questCenter.x, questCenter.y + 1f),
                strokeWidth = 2f
            )
            drawCircle(color = Color(0xFF00363D), radius = 1.2f, center = Offset(questCenter.x, questCenter.y + 3.5f))
        }
    }
}
