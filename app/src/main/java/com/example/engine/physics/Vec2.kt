package com.example.engine.physics

import androidx.compose.ui.geometry.Offset
import com.example.engine.core.math.FastBinaryMath
import kotlin.math.cos
import kotlin.math.sin

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════════╗
 * ║  ⚡ VEC2 & TRANSFORM MATRIX CORE (ZERO-ALLOCATION IN-PLACE MATH ENGINE)                    ║
 * ║  Engineered for ARM Cortex-A55 / Mobile Chipsets (Unisoc T603, Helio G36, Snapdragon)    ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════════╝
 */
class Vec2(var x: Float = 0f, var y: Float = 0f) {

    fun set(newX: Float, newY: Float): Vec2 {
        this.x = newX
        this.y = newY
        return this
    }

    fun set(other: Vec2): Vec2 {
        this.x = other.x
        this.y = other.y
        return this
    }

    fun add(other: Vec2): Vec2 {
        this.x += other.x
        this.y += other.y
        return this
    }

    fun add(ox: Float, oy: Float): Vec2 {
        this.x += ox
        this.y += oy
        return this
    }

    fun sub(other: Vec2): Vec2 {
        this.x -= other.x
        this.y -= other.y
        return this
    }

    fun sub(ox: Float, oy: Float): Vec2 {
        this.x -= ox
        this.y -= oy
        return this
    }

    fun scale(s: Float): Vec2 {
        this.x *= s
        this.y *= s
        return this
    }

    fun lengthSq(): Float = x * x + y * y

    fun length(): Float {
        val lenSq = lengthSq()
        return if (lenSq <= 0.000001f) 0f else FastBinaryMath.fastSqrt(lenSq)
    }

    fun normalize(): Vec2 {
        val lenSq = lengthSq()
        if (lenSq > 0.000001f) {
            val inv = FastBinaryMath.fastInvSqrt(lenSq)
            this.x *= inv
            this.y *= inv
        } else {
            this.x = 0f
            this.y = 0f
        }
        return this
    }

    fun dot(other: Vec2): Float = x * other.x + y * other.y
    fun dot(ox: Float, oy: Float): Float = x * ox + y * oy

    fun cross(other: Vec2): Float = x * other.y - y * other.x
    fun cross(ox: Float, oy: Float): Float = x * oy - y * ox

    fun rotate(angleRad: Float): Vec2 {
        val c = cos(angleRad)
        val s = sin(angleRad)
        val nx = x * c - y * s
        val ny = x * s + y * c
        this.x = nx
        this.y = ny
        return this
    }

    fun perp(): Vec2 {
        val temp = x
        this.x = -y
        this.y = temp
        return this
    }

    fun sanitize(): Vec2 {
        if (x.isNaN() || x.isInfinite()) x = 0f
        if (y.isNaN() || y.isInfinite()) y = 0f
        return this
    }

    fun toOffset(): Offset = Offset(x, y)

    companion object {
        fun add(a: Vec2, b: Vec2, out: Vec2): Vec2 {
            out.x = a.x + b.x
            out.y = a.y + b.y
            return out
        }

        fun sub(a: Vec2, b: Vec2, out: Vec2): Vec2 {
            out.x = a.x - b.x
            out.y = a.y - b.y
            return out
        }

        fun scale(a: Vec2, s: Float, out: Vec2): Vec2 {
            out.x = a.x * s
            out.y = a.y * s
            return out
        }

        fun cross(v: Vec2, s: Float, out: Vec2): Vec2 {
            out.x = s * v.y
            out.y = -s * v.x
            return out
        }

        fun cross(s: Float, v: Vec2, out: Vec2): Vec2 {
            out.x = -s * v.y
            out.y = s * v.x
            return out
        }

        fun dot(a: Vec2, b: Vec2): Float = a.x * b.x + a.y * b.y
        fun cross(a: Vec2, b: Vec2): Float = a.x * b.y - a.y * b.x
    }
}

/**
 * 2x2 Rotation & Inertia Matrix
 */
class Mat22 {
    var m00: Float = 1f; var m01: Float = 0f
    var m10: Float = 0f; var m11: Float = 1f

    fun set(angleRad: Float): Mat22 {
        val c = cos(angleRad)
        val s = sin(angleRad)
        m00 = c; m01 = -s
        m10 = s; m11 = c
        return this
    }

    fun mul(v: Vec2, out: Vec2): Vec2 {
        val x = m00 * v.x + m01 * v.y
        val y = m10 * v.x + m11 * v.y
        out.x = x
        out.y = y
        return out
    }

    fun mulTranspose(v: Vec2, out: Vec2): Vec2 {
        val x = m00 * v.x + m10 * v.y
        val y = m01 * v.x + m11 * v.y
        out.x = x
        out.y = y
        return out
    }
}

/**
 * Transform2D represents 2D Position + Orientation
 */
class Transform2D(
    val position: Vec2 = Vec2(),
    var angle: Float = 0f
) {
    val rotation: Mat22 = Mat22()

    fun updateRotation() {
        rotation.set(angle)
    }

    fun transform(localPoint: Vec2, out: Vec2): Vec2 {
        rotation.mul(localPoint, out)
        out.x += position.x
        out.y += position.y
        return out
    }

    fun inverseTransform(worldPoint: Vec2, out: Vec2): Vec2 {
        val tx = worldPoint.x - position.x
        val ty = worldPoint.y - position.y
        out.x = tx
        out.y = ty
        rotation.mulTranspose(out, out)
        return out
    }
}

/**
 * Axis-Aligned Bounding Box (AABB) for Fast Broadphase Discard
 */
class AABB(
    val min: Vec2 = Vec2(),
    val max: Vec2 = Vec2()
) {
    fun overlaps(other: AABB): Boolean {
        if (max.x < other.min.x || min.x > other.max.x) return false
        if (max.y < other.min.y || min.y > other.max.y) return false
        return true
    }

    fun contains(point: Vec2): Boolean {
        return point.x in min.x..max.x && point.y in min.y..max.y
    }
}
