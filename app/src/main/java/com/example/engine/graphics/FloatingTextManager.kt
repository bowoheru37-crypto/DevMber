package com.example.engine.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class FloatingText(
    var text: String = "",
    var x: Float = 0f,
    var y: Float = 0f,
    var vy: Float = -60f,
    var alpha: Float = 1.0f,
    var scale: Float = 1.0f,
    var color: Color = Color(0xFFFFB703),
    var lifetime: Float = 1.0f,
    var maxLifetime: Float = 1.0f,
    var isActive: Boolean = false
)

/**
 * Zero-allocation pool manager for floating score and event popups.
 * Safe for low-entry hardware (Android 5+ / itel A70).
 */
class FloatingTextManager(val poolSize: Int = 24) {
    val pool = Array(poolSize) { FloatingText() }

    fun spawn(
        text: String,
        pos: Offset,
        color: Color = Color(0xFFFFB703),
        initialScale: Float = 1.2f,
        lifetime: Float = 0.8f
    ) {
        for (item in pool) {
            if (!item.isActive) {
                item.text = text
                item.x = pos.x
                item.y = pos.y
                item.vy = -75f
                item.alpha = 1.0f
                item.scale = initialScale
                item.color = color
                item.lifetime = lifetime
                item.maxLifetime = lifetime
                item.isActive = true
                return
            }
        }
        // If all active, recycle first
        val fallback = pool[0]
        fallback.text = text
        fallback.x = pos.x
        fallback.y = pos.y
        fallback.vy = -75f
        fallback.alpha = 1.0f
        fallback.scale = initialScale
        fallback.color = color
        fallback.lifetime = lifetime
        fallback.maxLifetime = lifetime
        fallback.isActive = true
    }

    fun update(dt: Float) {
        val safeDt = dt.coerceIn(0.001f, 0.033f)
        for (item in pool) {
            if (item.isActive) {
                item.lifetime -= safeDt
                if (item.lifetime <= 0f) {
                    item.isActive = false
                } else {
                    item.y += item.vy * safeDt
                    val progress = item.lifetime / item.maxLifetime
                    item.alpha = progress.coerceIn(0f, 1f)
                    item.scale = 1.0f + (1f - progress) * 0.3f
                }
            }
        }
    }
}
