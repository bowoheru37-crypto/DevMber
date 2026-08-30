package com.example.engine.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.floor

/**
 * Broadphase Spatial Hash Grid.
 * Inspired by Box2D dynamic broadphase, Chipmunk2D spatial hash, and Godot 2D BVH.
 * Reduces collision query complexity from O(N^2) to O(1) average lookup time.
 * Engineered specifically for ARM Cortex-A55 mobile chips (itel A70) with fixed-size bucket arrays.
 */
class SpatialHashGrid(
    val cellSize: Float = 64f,
    val maxBuckets: Int = 128,
    val maxEntitiesPerBucket: Int = 16
) {
    // Flattened 2D array representation for cache line friendliness
    private val buckets = IntArray(maxBuckets * maxEntitiesPerBucket) { -1 }
    private val bucketCounts = IntArray(maxBuckets) { 0 }

    // Fast bitwise hash function
    private fun hash(gridX: Int, gridY: Int): Int {
        val h = (gridX * 73856093) xor (gridY * 19349663)
        return (h and 0x7FFFFFFF) % maxBuckets
    }

    fun clear() {
        for (i in 0 until maxBuckets) {
            bucketCounts[i] = 0
        }
    }

    /**
     * Insert an entity ID into the spatial grid based on its bounding box
     */
    fun insert(entityId: Int, x: Float, y: Float, width: Float, height: Float) {
        val minX = floor(x / cellSize).toInt()
        val maxX = floor((x + width) / cellSize).toInt()
        val minY = floor(y / cellSize).toInt()
        val maxY = floor((y + height) / cellSize).toInt()

        for (gx in minX..maxX) {
            for (gy in minY..maxY) {
                val bucketIdx = hash(gx, gy)
                val count = bucketCounts[bucketIdx]
                if (count < maxEntitiesPerBucket) {
                    buckets[bucketIdx * maxEntitiesPerBucket + count] = entityId
                    bucketCounts[bucketIdx] = count + 1
                }
            }
        }
    }

    /**
     * Query potential colliding entity candidates near a given bounding box
     */
    fun queryPotentialColliders(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        candidateOutput: IntArray,
        maxCandidates: Int
    ): Int {
        val minX = floor(x / cellSize).toInt()
        val maxX = floor((x + width) / cellSize).toInt()
        val minY = floor(y / cellSize).toInt()
        val maxY = floor((y + height) / cellSize).toInt()

        var candidateCount = 0

        for (gx in minX..maxX) {
            for (gy in minY..maxY) {
                val bucketIdx = hash(gx, gy)
                val count = bucketCounts[bucketIdx]
                val offset = bucketIdx * maxEntitiesPerBucket

                for (i in 0 until count) {
                    val candidateId = buckets[offset + i]
                    if (candidateId >= 0) {
                        // Check if already in candidateOutput to avoid duplicates
                        var alreadyAdded = false
                        for (k in 0 until candidateCount) {
                            if (candidateOutput[k] == candidateId) {
                                alreadyAdded = true
                                break
                            }
                        }
                        if (!alreadyAdded && candidateCount < maxCandidates) {
                            candidateOutput[candidateCount++] = candidateId
                        }
                    }
                }
            }
        }
        return candidateCount
    }
}
