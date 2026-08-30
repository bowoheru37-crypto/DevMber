package com.example.engine.core

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

/**
 * High-performance, zero-allocation Tweening & Easing Engine.
 * Inspired by DOTween, LibGDX Universal Tween Engine, and Phaser Tweens.
 * Optimized for low-end mobile devices (itel A70 / Unisoc T603).
 */
enum class EaseType {
    LINEAR,
    EASE_IN_QUAD,
    EASE_OUT_QUAD,
    EASE_IN_OUT_QUAD,
    EASE_IN_CUBIC,
    EASE_OUT_CUBIC,
    EASE_IN_OUT_CUBIC,
    EASE_OUT_BOUNCE,
    EASE_IN_BOUNCE,
    EASE_OUT_ELASTIC,
    EASE_IN_ELASTIC,
    EASE_OUT_BACK,
    EASE_IN_BACK,
    EASE_IN_OUT_SINE
}

class Tween(
    var id: Int = 0,
    var startValue: Float = 0f,
    var targetValue: Float = 0f,
    var duration: Float = 1f,
    var elapsed: Float = 0f,
    var easeType: EaseType = EaseType.LINEAR,
    var isRunning: Boolean = false,
    var isComplete: Boolean = false,
    var loopMode: Boolean = false,
    var pingPong: Boolean = false,
    var isReversed: Boolean = false,
    var onUpdate: ((Float) -> Unit)? = null,
    var onComplete: (() -> Unit)? = null
) {
    var currentValue: Float = 0f

    fun reset(
        start: Float,
        target: Float,
        dur: Float,
        ease: EaseType,
        loop: Boolean = false,
        pingpong: Boolean = false,
        updateCallback: (Float) -> Unit,
        completeCallback: (() -> Unit)? = null
    ) {
        startValue = start
        targetValue = target
        duration = if (dur > 0.001f) dur else 0.001f
        elapsed = 0f
        easeType = ease
        isRunning = true
        isComplete = false
        loopMode = loop
        pingPong = pingpong
        isReversed = false
        currentValue = start
        onUpdate = updateCallback
        onComplete = completeCallback
    }

    fun update(dt: Float) {
        if (!isRunning || isComplete) return

        elapsed += dt
        val progress = (elapsed / duration).coerceIn(0f, 1f)
        val easedProgress = evaluateEase(if (isReversed) 1f - progress else progress, easeType)

        currentValue = startValue + (targetValue - startValue) * easedProgress
        onUpdate?.invoke(currentValue)

        if (elapsed >= duration) {
            if (pingPong) {
                isReversed = !isReversed
                elapsed = 0f
            } else if (loopMode) {
                elapsed = 0f
            } else {
                isRunning = false
                isComplete = true
                onComplete?.invoke()
            }
        }
    }

    companion object {
        fun evaluateEase(t: Float, type: EaseType): Float {
            return when (type) {
                EaseType.LINEAR -> t
                EaseType.EASE_IN_QUAD -> t * t
                EaseType.EASE_OUT_QUAD -> t * (2f - t)
                EaseType.EASE_IN_OUT_QUAD -> if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
                EaseType.EASE_IN_CUBIC -> t * t * t
                EaseType.EASE_OUT_CUBIC -> {
                    val p = t - 1f
                    p * p * p + 1f
                }
                EaseType.EASE_IN_OUT_CUBIC -> if (t < 0.5f) 4f * t * t * t else (t - 1f) * (2f * t - 2f) * (2f * t - 2f) + 1f
                EaseType.EASE_OUT_BOUNCE -> easeOutBounce(t)
                EaseType.EASE_IN_BOUNCE -> 1f - easeOutBounce(1f - t)
                EaseType.EASE_OUT_ELASTIC -> {
                    if (t == 0f) 0f
                    else if (t == 1f) 1f
                    else {
                        val p = 0.3f
                        val s = p / 4f
                        2.0.pow(-10.0 * t).toFloat() * sin((t - s) * (2f * PI.toFloat()) / p) + 1f
                    }
                }
                EaseType.EASE_IN_ELASTIC -> {
                    if (t == 0f) 0f
                    else if (t == 1f) 1f
                    else {
                        val p = 0.3f
                        val s = p / 4f
                        val postFix = 2.0.pow(10.0 * (t - 1f)).toFloat()
                        -(postFix * sin((t - 1f - s) * (2f * PI.toFloat()) / p))
                    }
                }
                EaseType.EASE_OUT_BACK -> {
                    val s = 1.70158f
                    val p = t - 1f
                    p * p * ((s + 1f) * p + s) + 1f
                }
                EaseType.EASE_IN_BACK -> {
                    val s = 1.70158f
                    t * t * ((s + 1f) * t - s)
                }
                EaseType.EASE_IN_OUT_SINE -> -(cos(PI.toFloat() * t) - 1f) / 2f
            }
        }

        private fun easeOutBounce(t: Float): Float {
            return when {
                t < (1f / 2.75f) -> 7.5625f * t * t
                t < (2f / 2.75f) -> {
                    val p = t - (1.5f / 2.75f)
                    7.5625f * p * p + 0.75f
                }
                t < (2.5f / 2.75f) -> {
                    val p = t - (2.25f / 2.75f)
                    7.5625f * p * p + 0.9375f
                }
                else -> {
                    val p = t - (2.625f / 2.75f)
                    7.5625f * p * p + 0.984375f
                }
            }
        }
    }
}

/**
 * Global Zero-GC Pool of Reusable Tween instances
 */
class TweenEngine(maxTweens: Int = 32) {
    private val pool = Array(maxTweens) { Tween(id = it) }
    private var activeCount = 0

    fun to(
        start: Float,
        target: Float,
        duration: Float,
        ease: EaseType = EaseType.EASE_OUT_QUAD,
        loop: Boolean = false,
        pingpong: Boolean = false,
        onUpdate: (Float) -> Unit,
        onComplete: (() -> Unit)? = null
    ): Tween? {
        for (tween in pool) {
            if (!tween.isRunning) {
                tween.reset(start, target, duration, ease, loop, pingpong, onUpdate, onComplete)
                return tween
            }
        }
        return null
    }

    fun update(dt: Float) {
        var running = 0
        for (tween in pool) {
            if (tween.isRunning) {
                tween.update(dt)
                running++
            }
        }
        activeCount = running
    }

    fun clearAll() {
        for (tween in pool) {
            tween.isRunning = false
            tween.isComplete = true
        }
        activeCount = 0
    }

    fun getActiveCount(): Int = activeCount
}
