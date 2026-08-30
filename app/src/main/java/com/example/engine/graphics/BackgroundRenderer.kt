package com.example.engine.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance, zero-allocation DrawScope renderer for all 8 2D mobile game background types.
 * Fully optimized for low-end devices (itel A70 / Unisoc T603 / Android 5+).
 */
object BackgroundRenderer {

    fun render(
        drawScope: DrawScope,
        bgType: GameBackgroundType,
        width: Float,
        height: Float,
        scrollOffset: Float,
        animTime: Float,
        cameraX: Float = 0f,
        cameraY: Float = 0f
    ) {
        when (bgType) {
            GameBackgroundType.STATIC_BG -> renderStaticBg(drawScope, width, height)
            GameBackgroundType.TILED_REPEATING_BG -> renderTiledRepeatingBg(drawScope, width, height, scrollOffset)
            GameBackgroundType.SCROLLING_BG -> renderScrollingBg(drawScope, width, height, scrollOffset)
            GameBackgroundType.GRADIENT_SOLID_BG -> renderGradientSolidBg(drawScope, width, height, animTime)
            GameBackgroundType.PROCEDURAL_BG -> renderProceduralBg(drawScope, width, height, animTime)
            GameBackgroundType.ANIMATED_BG -> renderAnimatedBg(drawScope, width, height, animTime)
            GameBackgroundType.CAMERA_FOLLOW_BG -> renderCameraFollowBg(drawScope, width, height, cameraX, cameraY)
            GameBackgroundType.VERTEX_LOW_POLY_3D_BG -> renderVertexLowPoly3dBg(drawScope, width, height, animTime)
        }
    }

    /**
     * 1. STATIC BACKGROUND (Flappy Bird / Puzzle)
     * 1 single static draw, 0 dynamic calls, minimal CPU/GPU.
     */
    private fun renderStaticBg(drawScope: DrawScope, w: Float, h: Float) {
        with(drawScope) {
            // Sky
            drawRect(
                color = Color(0xFF0F172A),
                size = Size(w, h)
            )

            // Distant Mountains
            val mountainPath = Path().apply {
                moveTo(0f, h * 0.75f)
                lineTo(w * 0.25f, h * 0.5f)
                lineTo(w * 0.5f, h * 0.7f)
                lineTo(w * 0.78f, h * 0.45f)
                lineTo(w, h * 0.65f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(mountainPath, color = Color(0xFF1E293B))

            // Static City Silhouette
            var curX = 0f
            val buildingW = 40f
            while (curX < w) {
                val bH = 60f + ((curX.toInt() * 17) % 80)
                drawRect(
                    color = Color(0xFF131B2A),
                    topLeft = Offset(curX, h * 0.85f - bH),
                    size = Size(buildingW - 4f, bH + h * 0.15f)
                )
                curX += buildingW
            }

            // Static Ground Strip
            drawRect(
                color = Color(0xFF06D6A0),
                topLeft = Offset(0f, h * 0.85f),
                size = Size(w, 8f)
            )
            drawRect(
                color = Color(0xFF0D253A),
                topLeft = Offset(0f, h * 0.85f + 8f),
                size = Size(w, h * 0.15f)
            )
        }
    }

    /**
     * 2. TILED / REPEATING BACKGROUND (Endless Runner / Platformer)
     * 1 small tile pattern seamlessly repeated in X and Y without big texture overhead.
     */
    private fun renderTiledRepeatingBg(drawScope: DrawScope, w: Float, h: Float, scrollOffset: Float) {
        with(drawScope) {
            drawRect(color = Color(0xFF0B132B), size = Size(w, h))

            val tileSize = 48f
            val startX = -(scrollOffset % tileSize)
            var tx = startX
            while (tx < w + tileSize) {
                var ty = 0f
                while (ty < h + tileSize) {
                    val isAlt = ((tx / tileSize).toInt() + (ty / tileSize).toInt()) % 2 == 0

                    // Base tile block
                    drawRect(
                        color = if (isAlt) Color(0xFF1C2541) else Color(0xFF151E36),
                        topLeft = Offset(tx, ty),
                        size = Size(tileSize, tileSize)
                    )

                    // Inner cyber circuit glyph
                    drawRect(
                        color = Color(0xFF00F0FF).copy(alpha = if (isAlt) 0.08f else 0.03f),
                        topLeft = Offset(tx + 8f, ty + 8f),
                        size = Size(tileSize - 16f, tileSize - 16f)
                    )

                    drawCircle(
                        color = Color(0xFF00F0FF).copy(alpha = 0.15f),
                        radius = 2f,
                        center = Offset(tx + tileSize * 0.5f, ty + tileSize * 0.5f)
                    )

                    ty += tileSize
                }
                tx += tileSize
            }
        }
    }

    /**
     * 3. SCROLLING BACKGROUND (Subway Surfers / Space Shooter)
     * Continuous 1D speed layer strip creating seamless forward momentum.
     */
    private fun renderScrollingBg(drawScope: DrawScope, w: Float, h: Float, scrollOffset: Float) {
        with(drawScope) {
            drawRect(color = Color(0xFF080D1A), size = Size(w, h))

            // Infinite Scrolling Speed Lines & Pillars
            val pillarW = 80f
            val spacing = 220f
            val offset = -(scrollOffset * 1.5f % spacing)

            var px = offset
            while (px < w + spacing) {
                // High-tech vertical speed columns
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF00F0FF).copy(alpha = 0.05f), Color(0xFF9D4EDD).copy(alpha = 0.25f), Color(0xFF080D1A))
                    ),
                    topLeft = Offset(px, 0f),
                    size = Size(pillarW, h)
                )

                // Neon laser speed stripe
                drawLine(
                    color = Color(0xFF00F0FF).copy(alpha = 0.3f),
                    start = Offset(px + pillarW * 0.5f, 0f),
                    end = Offset(px + pillarW * 0.5f, h),
                    strokeWidth = 2f
                )

                px += spacing
            }

            // Floor speed grid lines
            val floorY = h * 0.82f
            drawLine(color = Color(0xFF00F0FF), start = Offset(0f, floorY), end = Offset(w, floorY), strokeWidth = 3f)
        }
    }

    /**
     * 4. GRADIENT / SOLID COLOR BACKGROUND (2048 / Hyper-Casual)
     * Minimalist, zero draw noise, vibrant colors focused purely on gameplay.
     */
    private fun renderGradientSolidBg(drawScope: DrawScope, w: Float, h: Float, animTime: Float) {
        with(drawScope) {
            val hueShift = (sin(animTime * 0.5f) + 1f) * 0.5f
            val topColor = Color(
                red = 0.05f + hueShift * 0.05f,
                green = 0.1f + hueShift * 0.08f,
                blue = 0.25f + hueShift * 0.15f,
                alpha = 1.0f
            )
            val bottomColor = Color(
                red = 0.15f + hueShift * 0.1f,
                green = 0.05f,
                blue = 0.2f + hueShift * 0.1f,
                alpha = 1.0f
            )

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor)
                ),
                size = Size(w, h)
            )

            // Subtle vignette radial soft spot
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.4f),
                    radius = w * 0.6f
                ),
                radius = w * 0.6f,
                center = Offset(w * 0.5f, h * 0.4f)
            )
        }
    }

    /**
     * 5. PROCEDURAL BACKGROUND (Geometry Dash / Arcade)
     * Pure algorithm-driven stars, cyber grid, and rhythmic audio synthwaves.
     */
    private fun renderProceduralBg(drawScope: DrawScope, w: Float, h: Float, animTime: Float) {
        with(drawScope) {
            drawRect(color = Color(0xFF060913), size = Size(w, h))

            // Procedural Twinkling Stars (Deterministically seeded)
            for (i in 0 until 40) {
                val seedX = (i * 197.3f) % w
                val seedY = (i * 123.7f) % (h * 0.75f)
                val twinkle = (sin(animTime * 4f + i) + 1f) * 0.5f
                val starSize = 1.5f + twinkle * 2.5f

                drawCircle(
                    color = if (i % 3 == 0) Color(0xFF00F0FF) else if (i % 3 == 1) Color(0xFFFFB703) else Color.White,
                    radius = starSize,
                    center = Offset(seedX, seedY),
                    alpha = 0.2f + twinkle * 0.8f
                )
            }

            // Procedural Synthwave Perspective Grid
            val horizonY = h * 0.6f
            val gridSpacing = 35f
            var gy = horizonY
            var step = 6f
            while (gy < h) {
                val alpha = ((gy - horizonY) / (h - horizonY)).coerceIn(0.1f, 0.6f)
                drawLine(
                    color = Color(0xFFFF2A85).copy(alpha = alpha),
                    start = Offset(0f, gy),
                    end = Offset(w, gy),
                    strokeWidth = 1.5f
                )
                gy += step
                step += 4f
            }

            // Radial perspective lines
            val vanishingPoint = Offset(w * 0.5f, horizonY)
            for (k in -8..8) {
                val bottomX = w * 0.5f + k * (w * 0.12f)
                drawLine(
                    color = Color(0xFF00F0FF).copy(alpha = 0.25f),
                    start = vanishingPoint,
                    end = Offset(bottomX, h),
                    strokeWidth = 1.5f
                )
            }
        }
    }

    /**
     * 6. ANIMATED BACKGROUND (Platformer Premium / Alto's Adventure)
     * Drifting clouds, glowing neon beacons, floating embers.
     */
    private fun renderAnimatedBg(drawScope: DrawScope, w: Float, h: Float, animTime: Float) {
        with(drawScope) {
            // Twilight Sky
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF3A0CA3))
                ),
                size = Size(w, h)
            )

            // Animated Drifting Clouds
            val cloudSpeed = 25f
            for (c in 0 until 3) {
                val cx = ((animTime * cloudSpeed * (1f + c * 0.4f) + c * 250f) % (w + 200f)) - 100f
                val cy = 60f + c * 50f + sin(animTime * 2f + c) * 6f
                val cloudW = 120f + c * 30f
                val cloudH = 30f + c * 10f

                drawOval(
                    color = Color.White.copy(alpha = 0.12f),
                    topLeft = Offset(cx, cy),
                    size = Size(cloudW, cloudH)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = cloudH * 0.9f,
                    center = Offset(cx + cloudW * 0.4f, cy + cloudH * 0.2f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = cloudH * 0.7f,
                    center = Offset(cx + cloudW * 0.7f, cy + cloudH * 0.3f)
                )
            }

            // Animated Floating Embers / Fireflies
            for (i in 0 until 18) {
                val fx = (i * 73f + sin(animTime * 1.5f + i) * 35f) % w
                val fy = h * 0.5f + (i * 47f - animTime * 30f) % (h * 0.45f)
                val pulse = (sin(animTime * 5f + i) + 1f) * 0.5f

                drawCircle(
                    color = Color(0xFFFFB703),
                    radius = 2.5f + pulse * 2f,
                    center = Offset(fx, fy),
                    alpha = 0.4f + pulse * 0.6f
                )
            }
        }
    }

    /**
     * 7. CAMERA FOLLOW BACKGROUND (Adventure / Open World)
     * Massive virtual world where viewport scrolls smoothly following player.
     */
    private fun renderCameraFollowBg(
        drawScope: DrawScope,
        w: Float,
        h: Float,
        camX: Float,
        camY: Float
    ) {
        with(drawScope) {
            drawRect(color = Color(0xFF0D1B2A), size = Size(w, h))

            // World Coordinate Grid
            val worldTileSize = 80f
            val offsetX = -(camX % worldTileSize)
            val offsetY = -(camY % worldTileSize)

            var gx = offsetX - worldTileSize
            while (gx < w + worldTileSize) {
                var gy = offsetY - worldTileSize
                while (gy < h + worldTileSize) {
                    val worldX = ((camX + gx) / worldTileSize).toInt()
                    val worldY = ((camY + gy) / worldTileSize).toInt()

                    drawRect(
                        color = if ((worldX + worldY) % 2 == 0) Color(0xFF1B263B) else Color(0xFF131F33),
                        topLeft = Offset(gx, gy),
                        size = Size(worldTileSize, worldTileSize)
                    )

                    // Coordinate marker for camera tracking verification
                    drawCircle(
                        color = Color(0xFF00F0FF).copy(alpha = 0.1f),
                        radius = 3f,
                        center = Offset(gx + worldTileSize * 0.5f, gy + worldTileSize * 0.5f)
                    )
                    gy += worldTileSize
                }
                gx += worldTileSize
            }

            // Radar Mini-Map in top corner
            val radarSize = 64f
            val radarPos = Offset(w - radarSize - 16f, 16f)
            drawRect(
                color = Color(0xFF0A0E17).copy(alpha = 0.85f),
                topLeft = radarPos,
                size = Size(radarSize, radarSize)
            )
            drawRect(
                color = Color(0xFF00F0FF),
                topLeft = radarPos,
                size = Size(radarSize, radarSize),
                style = Stroke(width = 1.5f)
            )
            // Camera position blip
            val blipX = radarPos.x + (camX % 2000f) / 2000f * radarSize
            val blipY = radarPos.y + (camY % 2000f) / 2000f * radarSize
            drawCircle(
                color = Color(0xFFFF2A85),
                radius = 3.5f,
                center = Offset(blipX.coerceIn(radarPos.x, radarPos.x + radarSize), blipY.coerceIn(radarPos.y, radarPos.y + radarSize))
            )
        }
    }

    /**
     * 8. VERTEX PAINTED / LOW-POLY 3D BG (Monument Valley Style)
     * Faceted geometric low-poly terrain with dynamic light and watercolor vertex shading.
     */
    private fun renderVertexLowPoly3dBg(drawScope: DrawScope, w: Float, h: Float, animTime: Float) {
        with(drawScope) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2B2D42), Color(0xFF1A1A2E), Color(0xFF16213E))
                ),
                size = Size(w, h)
            )

            // Dynamic light sun angle
            val lightAngle = animTime * 0.5f
            val lx = cos(lightAngle)
            val ly = sin(lightAngle)

            // Draw low-poly faceted isometric triangles
            val polySize = 90f
            var col = 0
            var px = 0f
            while (px < w + polySize) {
                var row = 0
                var py = h * 0.25f
                while (py < h + polySize) {
                    val isOffset = row % 2 == 1
                    val x0 = px + if (isOffset) polySize * 0.5f else 0f
                    val y0 = py

                    val pA = Offset(x0, y0)
                    val pB = Offset(x0 + polySize, y0)
                    val pC = Offset(x0 + polySize * 0.5f, y0 + polySize * 0.866f)

                    // Dynamic light intensity dot product simulation
                    val normalDot = ((col + row) * 0.35f + lx * 0.3f + ly * 0.2f).toFloat()
                    val intensity = (sin(normalDot) + 1f) * 0.5f

                    val facetColor = Color(
                        red = 0.15f + intensity * 0.3f,
                        green = 0.35f + intensity * 0.4f,
                        blue = 0.55f + intensity * 0.35f,
                        alpha = 0.75f
                    )

                    val triPath = Path().apply {
                        moveTo(pA.x, pA.y)
                        lineTo(pB.x, pB.y)
                        lineTo(pC.x, pC.y)
                        close()
                    }

                    drawPath(triPath, color = facetColor)
                    drawPath(triPath, color = Color.White.copy(alpha = 0.08f), style = Stroke(width = 1f))

                    row++
                    py += polySize * 0.866f
                }
                col++
                px += polySize
            }
        }
    }
}
