package com.example.engine.physics

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class BodyType {
    STATIC,
    DYNAMIC,
    KINEMATIC
}

enum class ShapeType {
    CIRCLE,
    BOX_OBB,
    POLYGON,
    CAPSULE
}

sealed class PhysicsShape2D(val type: ShapeType) {
    abstract fun computeMassAndInertia(density: Float, outMassInertia: FloatArray)
    abstract fun updateAABB(transform: Transform2D, outAABB: AABB)
}

class CircleShape2D(var radius: Float = 16f) : PhysicsShape2D(ShapeType.CIRCLE) {
    override fun computeMassAndInertia(density: Float, outMassInertia: FloatArray) {
        val area = Math.PI.toFloat() * radius * radius
        val mass = density * area
        val inertia = 0.5f * mass * radius * radius
        outMassInertia[0] = mass
        outMassInertia[1] = inertia
    }

    override fun updateAABB(transform: Transform2D, outAABB: AABB) {
        outAABB.min.set(transform.position.x - radius, transform.position.y - radius)
        outAABB.max.set(transform.position.x + radius, transform.position.y + radius)
    }
}

class BoxShape2D(var halfWidth: Float = 16f, var halfHeight: Float = 16f) : PhysicsShape2D(ShapeType.BOX_OBB) {
    val localVertices = arrayOf(
        Vec2(-halfWidth, -halfHeight),
        Vec2(halfWidth, -halfHeight),
        Vec2(halfWidth, halfHeight),
        Vec2(-halfWidth, halfHeight)
    )
    val worldVertices = arrayOf(Vec2(), Vec2(), Vec2(), Vec2())
    val worldNormals = arrayOf(Vec2(), Vec2(), Vec2(), Vec2())

    fun setDimensions(width: Float, height: Float) {
        halfWidth = width * 0.5f
        halfHeight = height * 0.5f
        localVertices[0].set(-halfWidth, -halfHeight)
        localVertices[1].set(halfWidth, -halfHeight)
        localVertices[2].set(halfWidth, halfHeight)
        localVertices[3].set(-halfWidth, halfHeight)
    }

    override fun computeMassAndInertia(density: Float, outMassInertia: FloatArray) {
        val mass = density * (4f * halfWidth * halfHeight)
        val inertia = (mass / 12f) * (4f * halfWidth * halfWidth + 4f * halfHeight * halfHeight)
        outMassInertia[0] = mass
        outMassInertia[1] = inertia
    }

    override fun updateAABB(transform: Transform2D, outAABB: AABB) {
        transform.updateRotation()
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (i in 0 until 4) {
            transform.transform(localVertices[i], worldVertices[i])
            val vx = worldVertices[i].x
            val vy = worldVertices[i].y
            minX = min(minX, vx)
            minY = min(minY, vy)
            maxX = max(maxX, vx)
            maxY = max(maxY, vy)
        }

        outAABB.min.set(minX, minY)
        outAABB.max.set(maxX, maxY)

        // Compute edge normals
        for (i in 0 until 4) {
            val next = (i + 1) % 4
            val edgeX = worldVertices[next].x - worldVertices[i].x
            val edgeY = worldVertices[next].y - worldVertices[i].y
            worldNormals[i].set(edgeY, -edgeX).normalize()
        }
    }
}

class CapsuleShape2D(
    var radius: Float = 12f,
    var length: Float = 32f // distance between two cap centers
) : PhysicsShape2D(ShapeType.CAPSULE) {
    val halfLength = length * 0.5f

    override fun computeMassAndInertia(density: Float, outMassInertia: FloatArray) {
        val rectArea = length * (2f * radius)
        val circleArea = Math.PI.toFloat() * radius * radius
        val mass = density * (rectArea + circleArea)
        val inertia = (1f / 12f) * mass * (length * length + 4f * radius * radius)
        outMassInertia[0] = mass
        outMassInertia[1] = inertia
    }

    override fun updateAABB(transform: Transform2D, outAABB: AABB) {
        val ext = halfLength + radius
        outAABB.min.set(transform.position.x - ext, transform.position.y - ext)
        outAABB.max.set(transform.position.x + ext, transform.position.y + ext)
    }
}

class PolygonShape2D(
    val localVertices: List<Vec2>
) : PhysicsShape2D(ShapeType.POLYGON) {
    val vertexCount = localVertices.size
    val worldVertices = Array(vertexCount) { Vec2() }
    val worldNormals = Array(vertexCount) { Vec2() }

    override fun computeMassAndInertia(density: Float, outMassInertia: FloatArray) {
        var area = 0f
        var inertia = 0f
        val kInv3 = 1f / 3f

        for (i in 0 until vertexCount) {
            val next = (i + 1) % vertexCount
            val p1 = localVertices[i]
            val p2 = localVertices[next]
            val d = p1.cross(p2)
            val triArea = 0.5f * d
            area += triArea

            val intX2 = p1.x * p1.x + p2.x * p1.x + p2.x * p2.x
            val intY2 = p1.y * p1.y + p2.y * p1.y + p2.y * p2.y
            inertia += (0.25f * kInv3 * d) * (intX2 + intY2)
        }

        val mass = density * abs(area)
        outMassInertia[0] = mass
        outMassInertia[1] = abs(density * inertia)
    }

    override fun updateAABB(transform: Transform2D, outAABB: AABB) {
        transform.updateRotation()
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (i in 0 until vertexCount) {
            transform.transform(localVertices[i], worldVertices[i])
            val vx = worldVertices[i].x
            val vy = worldVertices[i].y
            minX = min(minX, vx)
            minY = min(minY, vy)
            maxX = max(maxX, vx)
            maxY = max(maxY, vy)
        }

        outAABB.min.set(minX, minY)
        outAABB.max.set(maxX, maxY)

        for (i in 0 until vertexCount) {
            val next = (i + 1) % vertexCount
            val edgeX = worldVertices[next].x - worldVertices[i].x
            val edgeY = worldVertices[next].y - worldVertices[i].y
            worldNormals[i].set(edgeY, -edgeX).normalize()
        }
    }
}

/**
 * Typealias and Factory mapping RigidBody2D to the comprehensive PhysicsBody sealed class hierarchy
 */
typealias RigidBody2D = PhysicsBody

fun RigidBody2D(
    id: String = "body_${System.nanoTime()}",
    bodyType: BodyType = BodyType.DYNAMIC,
    shape: PhysicsShape2D = BoxShape2D(16f, 16f)
): PhysicsBody {
    return when (bodyType) {
        BodyType.STATIC -> PhysicsBody.Static(id = id, shape = shape)
        BodyType.DYNAMIC -> PhysicsBody.Dynamic(id = id, shape = shape)
        BodyType.KINEMATIC -> PhysicsBody.Kinematic(id = id, shape = shape)
    }
}
