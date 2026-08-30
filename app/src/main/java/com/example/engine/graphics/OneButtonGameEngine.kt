package com.example.engine.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.engine.AudioSynthesizer
import com.example.engine.character.CharacterClass
import com.example.engine.character.EnemyType
import com.example.engine.character.NpcEntity
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class GameCollectible(
    val id: Int,
    var x: Float,
    var y: Float,
    val type: String, // COIN, CRYSTAL, ENEMY, SPIKE, NPC
    val size: Float = 22f,
    var isCollected: Boolean = false,
    var enemyType: EnemyType = EnemyType.SLIME_BOUNCER,
    var npcData: NpcEntity? = null
)

/**
 * High-performance 1-Button Mobile Game Engine.
 * Supports:
 * - Character Archetypes (Cyber Ninja, Solar Knight, Void Warlock, Mecha Titan)
 * - Enemy AI Archetypes (Slime Bouncer, Cyber Drone, Void Stalker, Mecha Boss)
 * - Friendly Interactive NPC Quests & Dialogues
 * - Tap & Hold Mechanics (Jump, Flap, Dash, Gravity flip)
 * - Item spawning, dynamic platform physics, score multiplier, zero-allocation floating combat popups and sound synthesis.
 */
class OneButtonGameEngine {

    // Player Archetype & State
    var characterClass: CharacterClass = CharacterClass.CYBER_NINJA
    var playerX: Float = 140f
    var playerY: Float = 300f
    var playerVx: Float = 0f
    var playerVy: Float = 0f
    var playerRadius: Float = 18f
    var isGrounded: Boolean = false
    var isDashing: Boolean = false
    var dashTimer: Float = 0f
    var jumpCount: Int = 0
    var maxJumps: Int = 2
    var health: Int = 3
    var maxHealth: Int = 3
    var energy: Float = 100f
    var isAlive: Boolean = true
    var shieldActive: Boolean = false

    // Controls mode
    var controlMode: String = "JUMP" // JUMP (Flappy/Mario), DASH (Fast Forward), GRAVITY (Flip Gravity)
    var gravityDirection: Float = 1.0f

    // Camera & World Bounds
    var cameraX: Float = 0f
    var cameraY: Float = 0f
    var worldWidth: Float = 3000f
    var worldHeight: Float = 1600f
    var scrollOffset: Float = 0f
    var globalAnimTime: Float = 0f

    // Items, Enemies & NPCs
    val items = ArrayList<GameCollectible>(32)
    private var nextItemId = 1
    val floatingTexts = FloatingTextManager()

    // Game Stats
    var score: Int = 0
    var highScore: Int = 0
    var combo: Int = 1
    var comboTimer: Float = 0f
    var currentBgType: GameBackgroundType = GameBackgroundType.STATIC_BG

    fun setPlayerClass(archetype: CharacterClass) {
        characterClass = archetype
        maxHealth = archetype.baseHp
        health = archetype.baseHp
        maxJumps = if (archetype == CharacterClass.CYBER_NINJA) 2 else 1
        shieldActive = archetype == CharacterClass.SOLAR_KNIGHT
        AudioSynthesizer.playToneAsync(523.25f, durationMs = 80, volume = 0.25f)
    }

    fun reset(screenW: Float, screenH: Float) {
        playerX = 140f
        playerY = screenH * 0.5f
        playerVx = 0f
        playerVy = 0f
        isGrounded = false
        isDashing = false
        dashTimer = 0f
        jumpCount = 0
        maxHealth = characterClass.baseHp
        health = maxHealth
        energy = 100f
        isAlive = true
        score = 0
        combo = 1
        comboTimer = 0f
        gravityDirection = 1.0f
        scrollOffset = 0f
        cameraX = 0f
        cameraY = 0f
        shieldActive = characterClass == CharacterClass.SOLAR_KNIGHT

        spawnInitialWorld(screenW, screenH)
    }

    private fun spawnInitialWorld(screenW: Float, screenH: Float) {
        items.clear()
        val floorY = screenH * 0.82f

        val npcList = listOf(
            NpcEntity("npc_1", "Cyber Oracle", "Guide", Color(0xFF00F0FF), "Hold dash to smash drones!"),
            NpcEntity("npc_2", "Elder Aethel", "Alchemist", Color(0xFFFFB703), "Purple crystals boost special energy!"),
            NpcEntity("npc_3", "Aria Starweaver", "Mage", Color(0xFF9D4EDD), "Antigravity flips preserve horizontal velocity!")
        )

        for (i in 0 until 16) {
            val ix = 300f + i * 220f
            val itemType = when (i % 5) {
                0 -> "COIN"
                1 -> "CRYSTAL"
                2 -> "ENEMY"
                3 -> "NPC"
                else -> "SPIKE"
            }

            val eType = when (i % 4) {
                0 -> EnemyType.SLIME_BOUNCER
                1 -> EnemyType.CYBER_DRONE
                2 -> EnemyType.VOID_STALKER
                else -> EnemyType.MECHA_BOSS
            }

            val iy = when (itemType) {
                "SPIKE" -> floorY - 10f
                "ENEMY" -> if (eType.isFlying) floorY - 110f - (i % 2) * 40f else floorY - 24f
                "NPC" -> floorY - 26f
                "CRYSTAL" -> floorY - 90f - (i % 3) * 30f
                else -> floorY - 60f - (i % 2) * 40f
            }

            val npcData = if (itemType == "NPC") npcList[i % npcList.size] else null

            items.add(
                GameCollectible(
                    id = nextItemId++,
                    x = ix,
                    y = iy,
                    type = itemType,
                    size = if (itemType == "CRYSTAL") 26f else if (itemType == "NPC") 28f else 22f,
                    enemyType = eType,
                    npcData = npcData
                )
            )
        }
    }

    /**
     * Primary 1-Button Touch Action (Tap / Hold trigger)
     */
    fun onOneButtonAction() {
        if (!isAlive) {
            reset(800f, 1200f)
            return
        }

        when (controlMode) {
            "GRAVITY" -> {
                gravityDirection *= -1f
                playerVy = -200f * gravityDirection
                AudioSynthesizer.playToneAsync(440f, durationMs = 60, volume = 0.2f)
                floatingTexts.spawn("GRAVITY FLIP!", Offset(playerX, playerY - 30f), Color(0xFF9D4EDD))
            }
            "DASH" -> {
                if (energy >= 25f) {
                    energy -= 25f
                    isDashing = true
                    dashTimer = 0.25f
                    playerVx = 380f
                    AudioSynthesizer.playToneAsync(740f, durationMs = 90, volume = 0.25f)
                    floatingTexts.spawn(characterClass.skillName, Offset(playerX, playerY - 30f), characterClass.primaryColor)
                }
            }
            else -> { // Default JUMP / FLAP
                if (jumpCount < maxJumps) {
                    jumpCount++
                    playerVy = -480f * gravityDirection
                    isGrounded = false
                    val noteFreq = if (jumpCount == 1) 523.25f else 659.25f
                    AudioSynthesizer.playToneAsync(noteFreq, durationMs = 80, volume = 0.2f)
                    floatingTexts.spawn(if (jumpCount == 1) "JUMP!" else "DOUBLE JUMP!", Offset(playerX, playerY - 25f), Color(0xFF06D6A0))
                }
            }
        }
    }

    fun update(dt: Float, screenW: Float, screenH: Float) {
        val safeDt = dt.coerceIn(0.001f, 0.033f)
        globalAnimTime += safeDt

        if (!isAlive) return

        val floorY = screenH * 0.82f
        val ceilY = 40f
        val baseSpeed = characterClass.baseSpeed
        val speedForward = if (isDashing) baseSpeed * 2.1f else baseSpeed

        scrollOffset += speedForward * safeDt

        // 1. Dash timer and energy recharge
        if (isDashing) {
            dashTimer -= safeDt
            if (dashTimer <= 0f) {
                isDashing = false
                playerVx = 0f
            }
        }
        energy = (energy + safeDt * 20f).coerceAtMost(100f)

        // 2. Physics & Gravity
        val baseGravity = 1100f * gravityDirection
        playerVy += baseGravity * safeDt

        playerY += playerVy * safeDt
        playerX = (playerX + playerVx * safeDt).coerceIn(40f, screenW * 0.6f)

        // Camera Smooth Follow Lerp (For CAMERA_FOLLOW_BG)
        val targetCamX = (playerX + scrollOffset) - screenW * 0.35f
        val targetCamY = playerY - screenH * 0.5f
        cameraX += (targetCamX - cameraX) * 4f * safeDt
        cameraY += (targetCamY - cameraY) * 3f * safeDt

        // 3. Ground / Ceiling Collisions
        if (gravityDirection > 0) {
            if (playerY + playerRadius >= floorY) {
                playerY = floorY - playerRadius
                playerVy = 0f
                isGrounded = true
                jumpCount = 0
            }
            if (playerY - playerRadius <= ceilY) {
                playerY = ceilY + playerRadius
                playerVy = 0f
            }
        } else {
            if (playerY - playerRadius <= ceilY) {
                playerY = ceilY + playerRadius
                playerVy = 0f
                isGrounded = true
                jumpCount = 0
            }
            if (playerY + playerRadius >= floorY) {
                playerY = floorY - playerRadius
                playerVy = 0f
            }
        }

        // 4. Update and Recycle Items & Enemies
        val effectivePlayerX = playerX + scrollOffset

        for (item in items) {
            // Animate flying enemy sine-wave motion
            if (item.type == "ENEMY" && item.enemyType.isFlying) {
                item.y += sin(globalAnimTime * 5f + item.id) * 1.2f
            }

            if (item.isCollected) continue

            val dx = effectivePlayerX - item.x
            val dy = playerY - item.y
            val dist = sqrt(dx * dx + dy * dy)
            val hitThreshold = playerRadius + item.size * 0.6f

            if (dist < hitThreshold) {
                item.isCollected = true
                handleItemPickup(item, playerX, playerY)
            }

            // Recycle off-screen items to the right for endless progression
            if (item.x < effectivePlayerX - screenW * 0.5f) {
                item.x += screenW * 1.5f + Random.nextFloat() * 200f
                item.isCollected = false
            }
        }

        // 5. Combo Decay
        if (combo > 1) {
            comboTimer += safeDt
            if (comboTimer > 2.5f) {
                combo = 1
                comboTimer = 0f
            }
        }

        // 6. Floating texts
        floatingTexts.update(safeDt)
    }

    private fun handleItemPickup(item: GameCollectible, px: Float, py: Float) {
        when (item.type) {
            "COIN" -> {
                val pts = 100 * combo
                score += pts
                combo = (combo + 1).coerceAtMost(8)
                comboTimer = 0f
                if (score > highScore) highScore = score
                AudioSynthesizer.playToneAsync(880f, durationMs = 70, volume = 0.22f)
                floatingTexts.spawn("+$pts", Offset(px, py - 20f), Color(0xFFFFB703))
            }
            "CRYSTAL" -> {
                val pts = 300 * combo
                score += pts
                energy = (energy + 40f).coerceAtMost(100f)
                combo = (combo + 2).coerceAtMost(8)
                comboTimer = 0f
                if (score > highScore) highScore = score
                AudioSynthesizer.playToneAsync(1046.5f, durationMs = 120, volume = 0.25f)
                floatingTexts.spawn("+$pts CRYSTAL!", Offset(px, py - 25f), Color(0xFF9D4EDD), initialScale = 1.4f)
            }
            "NPC" -> {
                val npc = item.npcData
                if (npc != null) {
                    val reward = npc.questRewardScore
                    score += reward
                    if (score > highScore) highScore = score
                    AudioSynthesizer.playToneAsync(1318.5f, durationMs = 150, volume = 0.3f)
                    floatingTexts.spawn("${npc.name}: +$reward!", Offset(px, py - 35f), npc.color, initialScale = 1.4f)
                }
            }
            "ENEMY" -> {
                if (isDashing || characterClass == CharacterClass.MECHA_TITAN) {
                    // Smashed enemy during dash or heavy mecha collision
                    val pts = 500 * combo
                    score += pts
                    AudioSynthesizer.playToneAsync(600f, durationMs = 100, volume = 0.3f)
                    floatingTexts.spawn("SMASH! +$pts", Offset(px, py - 25f), Color(0xFF00F0FF), initialScale = 1.5f)
                } else if (shieldActive) {
                    // Shield absorbed damage
                    shieldActive = false
                    AudioSynthesizer.playToneAsync(700f, durationMs = 80, volume = 0.25f)
                    floatingTexts.spawn("SHIELD BLOCKED!", Offset(px, py - 30f), Color(0xFFFFB703))
                } else {
                    takeDamage(px, py, item.enemyType.damage)
                }
            }
            "SPIKE" -> {
                takeDamage(px, py, 1)
            }
        }
    }

    private fun takeDamage(px: Float, py: Float, dmg: Int = 1) {
        health -= dmg
        combo = 1
        comboTimer = 0f
        AudioSynthesizer.playToneAsync(150f, durationMs = 150, volume = 0.35f)
        floatingTexts.spawn("HIT! -$dmg HP", Offset(px, py - 30f), Color(0xFFFF4D6D), initialScale = 1.4f)

        if (health <= 0) {
            health = 0
            isAlive = false
            AudioSynthesizer.playToneAsync(110f, durationMs = 300, volume = 0.4f)
            floatingTexts.spawn("GAME OVER - TAP TO RESTART", Offset(px, py - 40f), Color(0xFFFF2A85), initialScale = 1.6f, lifetime = 2.0f)
        }
    }
}
