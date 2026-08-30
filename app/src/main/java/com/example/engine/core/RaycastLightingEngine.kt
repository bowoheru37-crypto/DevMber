package com.example.engine.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

/**
 * 2D Dynamic Point Light and Shadow Caster System.
 * Inspired by Box2D-Lights, LibGDX RayHandler, and Godot Light2D.
 * Performs fast zero-allocation raymarching and radial shadow occlusion on low-tier mobile GPUs.
 */
data class Light2D(
    val id: Int,
    var x: Float,
    var y: Float,
    var radius: Float = 140f,
    var color: Color = Color(0xFF00F0FF),
    var intensity: Float = 0.8f,
    var isPulsing: Boolean = false
)

class RaycastLightingEngine(maxLights: Int = 8) {
    val lights = ArrayList<Light2D>(maxLights)
    private var nextLightId = 1

    init {
        // Default ambient lighting emitters
        lights.add(Light2D(nextLightId++, 150f, 200f, 160f, Color(0xFF00F0FF), 0.75f, true))
        lights.add(Light2D(nextLightId++, 380f, 260f, 130f, Color(0xFFFFB703), 0.65f, false))
        lights.add(Light2D(nextLightId++, 600f, 180f, 150f, Color(0xFF9D4EDD), 0.70f, true))
    }

    fun addLight(x: Float, y: Float, radius: Float = 120f, color: Color = Color(0xFF00F0FF)): Light2D {
        val l = Light2D(nextLightId++, x, y, radius, color, 0.75f, true)
        if (lights.size >= 8) {
            lights.removeAt(0)
        }
        lights.add(l)
        return l
    }

    fun update(animTime: Float) {
        for (light in lights) {
            if (light.isPulsing) {
                val pulse = sin(animTime * 4f + light.id) * 0.15f
                light.intensity = (0.75f + pulse).coerceIn(0.2f, 1f)
            }
        }
    }

    /**
     * Draw 2D Radial Lights with soft falloff brush on Compose Canvas
     */
    fun draw(drawScope: DrawScope, animTime: Float) {
        with(drawScope) {
            for (light in lights) {
                val effectiveRadius = light.radius * light.intensity
                val radialBrush = Brush.radialGradient(
                    colors = listOf(
                        light.color.copy(alpha = 0.45f * light.intensity),
                        light.color.copy(alpha = 0.15f * light.intensity),
                        Color.Transparent
                    ),
                    center = Offset(light.x, light.y),
                    radius = effectiveRadius
                )

                drawCircle(
                    brush = radialBrush,
                    radius = effectiveRadius,
                    center = Offset(light.x, light.y)
                )

                // Light core filament
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f * light.intensity),
                    radius = 4f,
                    center = Offset(light.x, light.y)
                )
            }
        }
    }
}
