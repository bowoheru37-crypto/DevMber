package com.example.engine.core.math

/**
 * Low-level Fast Binary and Bitwise Mathematics.
 * Inspired by Quake III Fast Inverse Square Root (John Carmack / Gary Tarolli),
 * Q16.16 Fixed-Point Arithmetic, and zero-allocation bit manipulation algorithms.
 * Engineered for ARM Cortex-A55 mobile chips (itel A70) to eliminate FPU bottlenecks.
 */
object FastBinaryMath {

    /**
     * Famous Fast Inverse Square Root (1 / sqrt(x)) using IEEE-754 bitcast magic constant.
     * Computes 1/sqrt(x) with only 1 Newton-Raphson iteration.
     */
    fun fastInvSqrt(x: Float): Float {
        if (x <= 0f) return 0f
        val xhalf = 0.5f * x
        var i = java.lang.Float.floatToIntBits(x)
        i = 0x5f3759df - (i shr 1)
        var y = java.lang.Float.intBitsToFloat(i)
        y *= (1.5f - xhalf * y * y) // 1st Newton-Raphson iteration
        return y
    }

    /**
     * Fast approximation of sqrt(x) = x * fastInvSqrt(x)
     */
    fun fastSqrt(x: Float): Float {
        if (x <= 0f) return 0f
        return x * fastInvSqrt(x)
    }

    /**
     * Fast Euclidean Distance between (x1, y1) and (x2, y2)
     */
    fun fastDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val distSq = dx * dx + dy * dy
        return fastSqrt(distSq)
    }

    /**
     * Fast Normalization of 2D Vector (nx, ny)
     */
    fun fastNormalize(x: Float, y: Float, outResult: FloatArray) {
        val distSq = x * x + y * y
        if (distSq <= 0.00001f) {
            outResult[0] = 0f
            outResult[1] = 0f
            return
        }
        val inv = fastInvSqrt(distSq)
        outResult[0] = x * inv
        outResult[1] = y * inv
    }

    // ==========================================
    // Fixed-Point Q16.16 Arithmetic (Deterministic Physics)
    // ==========================================
    const val FP_SHIFT = 16
    const val FP_ONE = 1 shl FP_SHIFT
    const val FP_HALF = 1 shl (FP_SHIFT - 1)

    fun floatToFixed(f: Float): Int = (f * FP_ONE).toInt()
    fun fixedToFloat(fp: Int): Float = fp.toFloat() / FP_ONE

    fun fixedMul(a: Int, b: Int): Int {
        return ((a.toLong() * b.toLong()) shr FP_SHIFT).toInt()
    }

    fun fixedDiv(a: Int, b: Int): Int {
        if (b == 0) return 0
        return ((a.toLong() shl FP_SHIFT) / b.toLong()).toInt()
    }

    // ==========================================
    // Bitwise Spatial Packing (2x 16-bit ints in a 32-bit int)
    // ==========================================
    fun packCoordinates(x: Short, y: Short): Int {
        return ((x.toInt() and 0xFFFF) shl 16) or (y.toInt() and 0xFFFF)
    }

    fun unpackX(packed: Int): Short {
        return ((packed shr 16) and 0xFFFF).toShort()
    }

    fun unpackY(packed: Int): Short {
        return (packed and 0xFFFF).toShort()
    }

    // ==========================================
    // Fast Pseudo-Random LFSR (Linear Feedback Shift Register)
    // ==========================================
    private var lfsrState = 0xACE1u

    fun nextLfsrRandom(): Float {
        var bit = ((lfsrState shr 0) xor (lfsrState shr 2) xor (lfsrState shr 3) xor (lfsrState shr 5)) and 1u
        lfsrState = (lfsrState shr 1) or (bit shl 15)
        return (lfsrState.toFloat() / 65535f)
    }
}
