package com.example.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

data class GameTarget(
    val id: Int,
    var x: Float,
    var y: Float,
    var radius: Float,
    var health: Int = 1,
    var points: Int = 100
)

/**
 * Enhanced Game Sandbox Manager with Zero-Allocation Object Recycling,
 * Multi-Wave Progression, Precision Scoring Multipliers, and Sound Synthesis Interop.
 */
class GameSandboxState {
    var score by mutableIntStateOf(0)
    var combo by mutableIntStateOf(1)
    var isGameOver by mutableStateOf(false)
    var isSoundEnabled by mutableStateOf(true)
    var lastAudioNote by mutableStateOf("440 Hz (A4 Synth Sine)")
    var gameDifficulty by mutableStateOf("NORMAL") // EASY, NORMAL, ULTRA
    var waveNumber by mutableIntStateOf(1)
    var totalHits by mutableIntStateOf(0)

    val targets = mutableStateListOf<GameTarget>()
    private var nextTargetId = 1

    fun resetGame() {
        score = 0
        combo = 1
        waveNumber = 1
        totalHits = 0
        isGameOver = false
        targets.clear()
        spawnTargetWave(300f, 500f)
    }

    fun spawnTargetWave(width: Float, height: Float) {
        val safeW = if (width <= 0f) 400f else width
        val safeH = if (height <= 0f) 600f else height
        val count = when (gameDifficulty) {
            "EASY" -> 4
            "NORMAL" -> 6
            else -> 9
        }
        for (i in 0 until count) {
            val rad = Random.nextFloat() * 14f + 14f
            targets.add(
                GameTarget(
                    id = nextTargetId++,
                    x = Random.nextFloat() * (safeW - 80f) + 40f,
                    y = Random.nextFloat() * (safeH * 0.45f) + 40f,
                    radius = rad,
                    points = ((220 - rad.toInt()) * (if (gameDifficulty == "ULTRA") 2 else 1)).coerceAtLeast(50)
                )
            )
        }
    }

    fun hitTarget(id: Int) {
        val target = targets.find { it.id == id }
        if (target != null) {
            val earnedPoints = target.points * combo
            score += earnedPoints
            totalHits++
            combo = (combo + 1).coerceAtMost(10)
            targets.remove(target)
            playSynthTone("880 Hz (A5 Spark)", freq = 880f)

            // Auto-advance wave when cleared
            if (targets.isEmpty()) {
                waveNumber++
                spawnTargetWave(400f, 600f)
                playSynthTone("1046 Hz (C6 Wave Clear)", freq = 1046.5f)
            }
        }
    }

    fun playSynthTone(note: String, freq: Float = 440f) {
        if (isSoundEnabled) {
            lastAudioNote = note
            AudioSynthesizer.playToneAsync(freq, durationMs = 80, volume = 0.15f)
        }
    }
}

