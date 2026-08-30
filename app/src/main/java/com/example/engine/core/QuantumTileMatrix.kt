package com.example.engine.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.engine.graphics.TileBiome
import kotlin.random.Random

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════════╗
 * ║  💎 QUANTUM AUTO-TILE MATRIX & ADJACENCY ENGINE (PATENTABLE BENCHMARK INNOVATION)       ║
 * ║  4-Bit Wang Bitmask Calculation (16 Cardinal Autotile Cases) with Zero Memory Allocations ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════════╝
 */
class QuantumTileMatrix(
    val width: Int = 32,
    val height: Int = 20,
    val tileSize: Float = 28f
) {
    // Continuous 1D primitive byte buffers: index = y * width + x
    val tileData = ByteArray(width * height) { 0 }
    val bitmaskData = ByteArray(width * height) { 0 }

    var currentBiome: TileBiome = TileBiome.NEON_CYBERPUNK

    fun clear() {
        tileData.fill(0)
        bitmaskData.fill(0)
    }

    fun setTile(x: Int, y: Int, isSolid: Boolean) {
        if (x in 0 until width && y in 0 until height) {
            tileData[y * width + x] = if (isSolid) 1.toByte() else 0.toByte()
        }
    }

    fun isSolid(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return true
        return tileData[y * width + x].toInt() == 1
    }

    /**
     * Compute 4-Bit Cardinal Neighbor Adjacency Mask:
     * Bit 0 (1): North, Bit 1 (2): East, Bit 2 (4): South, Bit 3 (8): West
     * Total = 16 Distinct Autotile Visual Configurations
     */
    fun computeBitmasks() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (!isSolid(x, y)) {
                    bitmaskData[y * width + x] = 0
                    continue
                }
                var mask = 0
                if (isSolid(x, y - 1)) mask = mask or 1
                if (isSolid(x + 1, y)) mask = mask or 2
                if (isSolid(x, y + 1)) mask = mask or 4
                if (isSolid(x - 1, y)) mask = mask or 8
                bitmaskData[y * width + x] = mask.toByte()
            }
        }
    }

    fun generateSamplePreset(presetType: String) {
        clear()
        when (presetType) {
            "SNAKE_ARENA" -> {
                currentBiome = TileBiome.EMERALD_RUINS
                // Outer bounding box
                for (x in 0 until width) {
                    setTile(x, 0, true)
                    setTile(x, height - 1, true)
                }
                for (y in 0 until height) {
                    setTile(0, y, true)
                    setTile(width - 1, y, true)
                }
                // Interior obstacle pillars
                for (x in 6..10) setTile(x, 6, true)
                for (x in 20..24) setTile(x, 14, true)
            }
            "FLAPPY_PIPES" -> {
                currentBiome = TileBiome.NEON_CYBERPUNK
                for (x in 0 until width) setTile(x, height - 1, true)
                // Pipe 1
                for (y in 0..6) setTile(8, y, true)
                for (y in 12 until height) setTile(8, y, true)
                // Pipe 2
                for (y in 0..4) setTile(18, y, true)
                for (y in 10 until height) setTile(18, y, true)
                // Pipe 3
                for (y in 0..8) setTile(28, y, true)
                for (y in 14 until height) setTile(28, y, true)
            }
            "PLATFORMER_CAVE" -> {
                currentBiome = TileBiome.CRYSTAL_CAVERNS
                val rand = Random(42L)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        if (x == 0 || x == width - 1 || y == 0 || y == height - 1 || (y > height - 4 && rand.nextFloat() < 0.8f)) {
                            setTile(x, y, true)
                        } else if (rand.nextFloat() < 0.25f) {
                            setTile(x, y, true)
                        }
                    }
                }
            }
            else -> { // DEFAULT DUNGEON
                currentBiome = TileBiome.SOLAR_CITADEL
                for (x in 0 until width) {
                    setTile(x, 0, true)
                    setTile(x, height - 1, true)
                }
                for (y in 0 until height) {
                    setTile(0, y, true)
                    setTile(width - 1, y, true)
                }
                for (x in 4 until width - 4 step 4) {
                    for (y in 4 until height - 4 step 3) {
                        setTile(x, y, true)
                    }
                }
            }
        }
        computeBitmasks()
    }

    /**
     * Batch Render all tiles using single DrawScope pass with 0 GC overhead.
     */
    fun render(drawScope: DrawScope, offsetX: Float = 0f, offsetY: Float = 0f) {
        with(drawScope) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val idx = y * width + x
                    if (tileData[idx].toInt() == 0) continue

                    val px = offsetX + x * tileSize
                    val py = offsetY + y * tileSize
                    val mask = bitmaskData[idx].toInt()

                    // Base Solid Tile Body
                    drawRect(
                        color = currentBiome.solidColor,
                        topLeft = Offset(px, py),
                        size = Size(tileSize, tileSize)
                    )

                    // Procedural Edge Highlights based on 4-bit bitmask
                    val hasNorth = (mask and 1) != 0
                    val hasEast = (mask and 2) != 0
                    val hasSouth = (mask and 4) != 0
                    val hasWest = (mask and 8) != 0

                    // Top Edge Highlight (Grass / Neon Rim)
                    if (!hasNorth) {
                        drawRect(
                            color = currentBiome.accentGlow,
                            topLeft = Offset(px, py),
                            size = Size(tileSize, 3f)
                        )
                    }
                    if (!hasSouth) {
                        drawRect(
                            color = currentBiome.highlightColor,
                            topLeft = Offset(px, py + tileSize - 2f),
                            size = Size(tileSize, 2f)
                        )
                    }
                    if (!hasWest) {
                        drawRect(
                            color = currentBiome.highlightColor,
                            topLeft = Offset(px, py),
                            size = Size(2f, tileSize)
                        )
                    }
                    if (!hasEast) {
                        drawRect(
                            color = currentBiome.highlightColor,
                            topLeft = Offset(px + tileSize - 2f, py),
                            size = Size(2f, tileSize)
                        )
                    }

                    // Inner Circuit Pattern
                    if (mask == 15) { // Center tile
                        drawCircle(
                            color = currentBiome.highlightColor,
                            radius = 2f,
                            center = Offset(px + tileSize * 0.5f, py + tileSize * 0.5f)
                        )
                    }
                }
            }
        }
    }
}
