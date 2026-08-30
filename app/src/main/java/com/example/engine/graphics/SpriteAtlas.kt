package com.example.engine.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Definition of a Region / Slice within a Texture Atlas
 */
data class AtlasRegion(
    val id: String,
    val uMin: Float,
    val vMin: Float,
    val uMax: Float,
    val vMax: Float,
    val originalWidth: Float,
    val originalHeight: Float
)

/**
 * Animated Sprite Controller for frame-based or transform-based animation
 */
class AnimatedSprite(
    val name: String,
    val frameCount: Int,
    val frameDuration: Float = 0.12f,
    val loop: Boolean = true
) {
    var currentFrame: Int = 0
    private var timer: Float = 0f

    fun update(dt: Float) {
        timer += dt
        if (timer >= frameDuration) {
            timer -= frameDuration
            if (currentFrame < frameCount - 1) {
                currentFrame++
            } else if (loop) {
                currentFrame = 0
            }
        }
    }

    fun reset() {
        currentFrame = 0
        timer = 0f
    }
}

/**
 * Procedural Vector Sprite & Tile Library
 * Zero image decoding overhead for extreme low-end device performance.
 */
object SpriteRenderer {

    /**
     * Render Hero Player Character (1-Button Avatar)
     */
    fun drawHeroPlayer(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        radius: Float = 20f,
        isJumping: Boolean = false,
        isDashing: Boolean = false,
        animTime: Float = 0f,
        tintColor: Color = Color(0xFF00F0FF)
    ) {
        with(drawScope) {
            val bounce = if (isJumping) -4f else (sin(animTime * 12f) * 2f)
            val center = Offset(x, y + bounce)

            // Dash Trail Glow
            if (isDashing) {
                drawCircle(
                    color = tintColor.copy(alpha = 0.35f),
                    radius = radius * 1.5f,
                    center = Offset(x - 25f, y + bounce)
                )
                drawCircle(
                    color = tintColor.copy(alpha = 0.2f),
                    radius = radius * 1.8f,
                    center = Offset(x - 50f, y + bounce)
                )
            }

            // Main Body Core
            drawCircle(
                color = tintColor,
                radius = radius,
                center = center
            )

            // Outer Shield / Cyber Rim
            drawCircle(
                color = Color.White,
                radius = radius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Dynamic Eye / Visor
            val eyeOffsetX = if (isDashing) 6f else 4f
            val eyeCenter = Offset(center.x + eyeOffsetX, center.y - 3f)
            drawCircle(
                color = Color(0xFF00363D),
                radius = radius * 0.35f,
                center = eyeCenter
            )
            drawCircle(
                color = Color.White,
                radius = radius * 0.15f,
                center = Offset(eyeCenter.x + 1.5f, eyeCenter.y - 1.5f)
            )

            // Propulsion Jet Flap / Particle Tail
            if (isJumping) {
                val flamePath = Path().apply {
                    moveTo(center.x - radius * 0.5f, center.y + radius * 0.8f)
                    lineTo(center.x, center.y + radius * 1.6f + sin(animTime * 30f) * 4f)
                    lineTo(center.x + radius * 0.5f, center.y + radius * 0.8f)
                    close()
                }
                drawPath(flamePath, color = Color(0xFFFFB703))
                drawPath(flamePath, color = Color(0xFFFF2A85), style = Stroke(width = 1.5f))
            }
        }
    }

    /**
     * Render Collectible Gold Coin (with 3D rotation projection)
     */
    fun drawGoldCoin(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        size: Float = 22f,
        animTime: Float = 0f
    ) {
        with(drawScope) {
            val scaleX = cos(animTime * 6f)
            val halfW = (size * 0.5f * kotlin.math.abs(scaleX)).coerceAtLeast(2f)
            val halfH = size * 0.5f

            // Coin outer rim
            drawOval(
                color = Color(0xFFFFB703),
                topLeft = Offset(x - halfW, y - halfH),
                size = Size(halfW * 2f, halfH * 2f)
            )
            // Coin inner star/symbol
            if (halfW > 5f) {
                drawOval(
                    color = Color(0xFFFFE169),
                    topLeft = Offset(x - halfW * 0.7f, y - halfH * 0.7f),
                    size = Size(halfW * 1.4f, halfH * 1.4f)
                )
            }
            // Outline
            drawOval(
                color = Color(0xFFD48B00),
                topLeft = Offset(x - halfW, y - halfH),
                size = Size(halfW * 2f, halfH * 2f),
                style = Stroke(width = 1.5f)
            )
        }
    }

    /**
     * Render Power Energy Gem / Crystal
     */
    fun drawPowerCrystal(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        size: Float = 24f,
        animTime: Float = 0f
    ) {
        with(drawScope) {
            val floatOffset = sin(animTime * 4f) * 3f
            val cy = y + floatOffset
            val s = size * 0.5f

            val diamondPath = Path().apply {
                moveTo(x, cy - s * 1.2f)
                lineTo(x + s, cy)
                lineTo(x, cy + s * 1.2f)
                lineTo(x - s, cy)
                close()
            }

            // Glow Aura
            drawCircle(
                color = Color(0xFF9D4EDD).copy(alpha = 0.3f),
                radius = size * 0.9f,
                center = Offset(x, cy)
            )

            // Gem fill & highlights
            drawPath(diamondPath, color = Color(0xFF9D4EDD))
            val topFacet = Path().apply {
                moveTo(x, cy - s * 1.2f)
                lineTo(x + s * 0.5f, cy)
                lineTo(x, cy)
                lineTo(x - s * 0.5f, cy)
                close()
            }
            drawPath(topFacet, color = Color(0xFFE0AAFF))
            drawPath(diamondPath, color = Color.White, style = Stroke(width = 1.5f))
        }
    }

    /**
     * Render Slime / Enemy Bouncer
     */
    fun drawSlimeEnemy(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        radius: Float = 18f,
        animTime: Float = 0f
    ) {
        with(drawScope) {
            val squish = sin(animTime * 8f) * 3f
            val w = radius + squish
            val h = radius - squish
            val center = Offset(x, y)

            // Slime Body
            drawOval(
                color = Color(0xFFFF2A85),
                topLeft = Offset(x - w, y - h),
                size = Size(w * 2f, h * 2f)
            )
            drawOval(
                color = Color(0xFFFF70A6),
                topLeft = Offset(x - w * 0.7f, y - h * 0.7f),
                size = Size(w * 1.4f, h * 1.4f)
            )
            drawOval(
                color = Color.White,
                topLeft = Offset(x - w, y - h),
                size = Size(w * 2f, h * 2f),
                style = Stroke(width = 1.5f)
            )

            // Angry Eyes
            drawCircle(color = Color.White, radius = 4f, center = Offset(x - 5f, y - 2f))
            drawCircle(color = Color(0xFF590D22), radius = 2f, center = Offset(x - 4f, y - 2f))

            drawCircle(color = Color.White, radius = 4f, center = Offset(x + 5f, y - 2f))
            drawCircle(color = Color(0xFF590D22), radius = 2f, center = Offset(x + 6f, y - 2f))
        }
    }

    /**
     * Render Hazard Spikes
     */
    fun drawHazardSpike(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        width: Float = 30f,
        height: Float = 24f
    ) {
        with(drawScope) {
            val spikePath = Path().apply {
                moveTo(x - width * 0.5f, y)
                lineTo(x, y - height)
                lineTo(x + width * 0.5f, y)
                close()
            }
            drawPath(spikePath, color = Color(0xFFFF4D6D))
            drawPath(spikePath, color = Color.White, style = Stroke(width = 1.5f))
        }
    }
}
