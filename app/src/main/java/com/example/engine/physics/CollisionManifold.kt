package com.example.engine.physics

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Single contact point with impulse cache for Warm Starting & Baumgarte stabilization
 */
class ContactPoint2D {
    val position: Vec2 = Vec2()
    val rA: Vec2 = Vec2() // Vector from body A center to contact point
    val rB: Vec2 = Vec2() // Vector from body B center to contact point
    var separation: Float = 0f

    var normalImpulse: Float = 0f
    var tangentImpulse: Float = 0f

    var normalMass: Float = 0f
    var tangentMass: Float = 0f
    var bias: Float = 0f
}

/**
 * Contact Manifold holding collision geometry between two RigidBody2D instances
 */
class ContactManifold2D {
    var bodyA: RigidBody2D? = null
    var bodyB: RigidBody2D? = null

    val normal: Vec2 = Vec2()
    val tangent: Vec2 = Vec2()

    var contactCount: Int = 0
    val contacts = arrayOf(ContactPoint2D(), ContactPoint2D())

    var friction: Float = 0.3f
    var restitution: Float = 0.5f

    fun reset() {
        bodyA = null
        bodyB = null
        normal.set(0f, 0f)
        tangent.set(0f, 0f)
        contactCount = 0
        contacts[0].normalImpulse = 0f
        contacts[0].tangentImpulse = 0f
        contacts[1].normalImpulse = 0f
        contacts[1].tangentImpulse = 0f
    }

    /**
     * Pre-step for Sequential Impulse Solver: calculates effective mass & Baumgarte bias
     */
    fun preStep(dt: Float, invDt: Float) {
        val a = bodyA ?: return
        val b = bodyB ?: return

        // Combined material properties
        friction = sqrt(a.friction * b.friction)
        restitution = max(a.restitution, b.restitution)

        tangent.set(-normal.y, normal.x)

        val kBaumgarte = 0.2f
        val kSlop = 0.5f // Positional penetration allowance (px)

        for (i in 0 until contactCount) {
            val c = contacts[i]

            c.rA.set(c.position.x - a.transform.position.x, c.position.y - a.transform.position.y)
            c.rB.set(c.position.x - b.transform.position.x, c.position.y - b.transform.position.y)

            // Normal Mass: 1 / (invMassA + invMassB + (rA x n)^2 / IA + (rB x n)^2 / IB)
            val rnA = c.rA.cross(normal)
            val rnB = c.rB.cross(normal)
            var kNormal = a.invMass + b.invMass + (rnA * rnA) * a.invInertia + (rnB * rnB) * b.invInertia
            c.normalMass = if (kNormal > 0f) 1.0f / kNormal else 0f

            // Tangent Mass
            val rtA = c.rA.cross(tangent)
            val rtB = c.rB.cross(tangent)
            var kTangent = a.invMass + b.invMass + (rtA * rtA) * a.invInertia + (rtB * rtB) * b.invInertia
            c.tangentMass = if (kTangent > 0f) 1.0f / kTangent else 0f

            // Relative contact velocity
            val vAx = a.linearVelocity.x - a.angularVelocity * c.rA.y
            val vAy = a.linearVelocity.y + a.angularVelocity * c.rA.x
            val vBx = b.linearVelocity.x - b.angularVelocity * c.rB.y
            val vBy = b.linearVelocity.y + b.angularVelocity * c.rB.x
            val relVx = vBx - vAx
            val relVy = vBy - vAy
            val normalVelocity = relVx * normal.x + relVy * normal.y

            // Restitution bias (elastic bounce only if relative velocity is above threshold)
            var restitutionBias = 0f
            if (normalVelocity < -20f) {
                restitutionBias = -restitution * normalVelocity
            }

            // Baumgarte position correction bias
            val penetration = (-c.separation - kSlop).coerceAtLeast(0f)
            c.bias = -kBaumgarte * invDt * penetration + restitutionBias

            // Warm Starting: Apply accumulated impulses from previous frame
            val pNx = normal.x * c.normalImpulse
            val pNy = normal.y * c.normalImpulse
            val pTx = tangent.x * c.tangentImpulse
            val pTy = tangent.y * c.tangentImpulse
            val pX = pNx + pTx
            val pY = pNy + pTy

            a.linearVelocity.x -= pX * a.invMass
            a.linearVelocity.y -= pY * a.invMass
            a.angularVelocity -= c.rA.cross(pX, pY) * a.invInertia

            b.linearVelocity.x += pX * b.invMass
            b.linearVelocity.y += pY * b.invMass
            b.angularVelocity += c.rB.cross(pX, pY) * b.invInertia
        }
    }

    /**
     * Solve Velocity Constraints (Normal Impulse + Coulomb Friction Cone)
     */
    fun solveVelocity() {
        val a = bodyA ?: return
        val b = bodyB ?: return

        for (i in 0 until contactCount) {
            val c = contacts[i]

            // 1. Solve Friction Constraint (Tangent)
            val vAx = a.linearVelocity.x - a.angularVelocity * c.rA.y
            val vAy = a.linearVelocity.y + a.angularVelocity * c.rA.x
            val vBx = b.linearVelocity.x - b.angularVelocity * c.rB.y
            val vBy = b.linearVelocity.y + b.angularVelocity * c.rB.x
            val relVx = vBx - vAx
            val relVy = vBy - vAy

            val tangentVelocity = relVx * tangent.x + relVy * tangent.y
            var deltaTangentImpulse = -tangentVelocity * c.tangentMass

            // Clamp friction to Coulomb's Law: |F_t| <= mu * F_n
            val maxFriction = friction * c.normalImpulse
            val oldTangentImpulse = c.tangentImpulse
            c.tangentImpulse = (oldTangentImpulse + deltaTangentImpulse).coerceIn(-maxFriction, maxFriction)
            deltaTangentImpulse = c.tangentImpulse - oldTangentImpulse

            val fTx = tangent.x * deltaTangentImpulse
            val fTy = tangent.y * deltaTangentImpulse

            a.linearVelocity.x -= fTx * a.invMass
            a.linearVelocity.y -= fTy * a.invMass
            a.angularVelocity -= c.rA.cross(fTx, fTy) * a.invInertia

            b.linearVelocity.x += fTx * b.invMass
            b.linearVelocity.y += fTy * b.invMass
            b.angularVelocity += c.rB.cross(fTx, fTy) * b.invInertia

            // 2. Solve Penetration & Restitution Constraint (Normal)
            val vAx2 = a.linearVelocity.x - a.angularVelocity * c.rA.y
            val vAy2 = a.linearVelocity.y + a.angularVelocity * c.rA.x
            val vBx2 = b.linearVelocity.x - b.angularVelocity * c.rB.y
            val vBy2 = b.linearVelocity.y + b.angularVelocity * c.rB.x
            val relVx2 = vBx2 - vAx2
            val relVy2 = vBy2 - vAy2

            val normalVelocity = relVx2 * normal.x + relVy2 * normal.y
            var deltaNormalImpulse = -(normalVelocity - c.bias) * c.normalMass

            // Clamp normal impulse: F_n >= 0 (bodies can only push, never pull)
            val oldNormalImpulse = c.normalImpulse
            c.normalImpulse = max(oldNormalImpulse + deltaNormalImpulse, 0f)
            deltaNormalImpulse = c.normalImpulse - oldNormalImpulse

            val fNx = normal.x * deltaNormalImpulse
            val fNy = normal.y * deltaNormalImpulse

            a.linearVelocity.x -= fNx * a.invMass
            a.linearVelocity.y -= fNy * a.invMass
            a.angularVelocity -= c.rA.cross(fNx, fNy) * a.invInertia

            b.linearVelocity.x += fNx * b.invMass
            b.linearVelocity.y += fNy * b.invMass
            b.angularVelocity += c.rB.cross(fNx, fNy) * b.invInertia
        }
    }
}

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════════╗
 * ║  ⚡ NARROWPHASE COLLISION DETECTOR (SEPARATING AXIS THEOREM & MANIFOLD CLIPPING)          ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════════╝
 */
object NarrowphaseCollision {

    /**
     * Circle vs Circle Manifold
     */
    fun collideCircles(a: RigidBody2D, circleA: CircleShape2D, b: RigidBody2D, circleB: CircleShape2D, manifold: ContactManifold2D): Boolean {
        val dx = b.transform.position.x - a.transform.position.x
        val dy = b.transform.position.y - a.transform.position.y
        val distSq = dx * dx + dy * dy
        val radiusSum = circleA.radius + circleB.radius

        if (distSq >= radiusSum * radiusSum) return false

        manifold.bodyA = a
        manifold.bodyB = b
        val dist = if (distSq > 0.0001f) sqrt(distSq) else 0f

        if (dist > 0f) {
            manifold.normal.set(dx / dist, dy / dist)
            manifold.contacts[0].separation = dist - radiusSum
            manifold.contacts[0].position.set(
                a.transform.position.x + manifold.normal.x * circleA.radius,
                a.transform.position.y + manifold.normal.y * circleA.radius
            )
        } else {
            manifold.normal.set(0f, -1f)
            manifold.contacts[0].separation = -radiusSum
            manifold.contacts[0].position.set(a.transform.position)
        }

        manifold.contactCount = 1
        return true
    }

    /**
     * Circle vs Oriented Box (OBB) Manifold
     */
    fun collideCircleBox(circleBody: RigidBody2D, circle: CircleShape2D, boxBody: RigidBody2D, box: BoxShape2D, manifold: ContactManifold2D, flip: Boolean = false): Boolean {
        // Transform circle center into box's local space
        val localCircle = Vec2()
        boxBody.transform.inverseTransform(circleBody.transform.position, localCircle)

        // Clamp local point to box half-extents to find closest point on box
        val closestX = localCircle.x.coerceIn(-box.halfWidth, box.halfWidth)
        val closestY = localCircle.y.coerceIn(-box.halfHeight, box.halfHeight)

        var inside = false
        var normalX = localCircle.x - closestX
        var normalY = localCircle.y - closestY
        var distSq = normalX * normalX + normalY * normalY

        // If circle center is inside the box, push out along shortest axis
        if (distSq < 0.000001f) {
            inside = true
            val dx = box.halfWidth - abs(localCircle.x)
            val dy = box.halfHeight - abs(localCircle.y)
            if (dx < dy) {
                normalX = if (localCircle.x > 0) 1f else -1f
                normalY = 0f
                distSq = dx * dx
            } else {
                normalX = 0f
                normalY = if (localCircle.y > 0) 1f else -1f
                distSq = dy * dy
            }
        }

        val dist = sqrt(distSq)
        val separation = if (inside) -dist - circle.radius else dist - circle.radius
        if (separation >= 0f) return false

        // Transform normal to world space
        val worldNormal = Vec2(normalX / dist, normalY / dist)
        boxBody.transform.rotation.mul(worldNormal, worldNormal)

        if (flip) {
            manifold.bodyA = boxBody
            manifold.bodyB = circleBody
            manifold.normal.set(-worldNormal.x, -worldNormal.y)
        } else {
            manifold.bodyA = circleBody
            manifold.bodyB = boxBody
            manifold.normal.set(worldNormal.x, worldNormal.y)
        }

        val contactWorld = Vec2(closestX, closestY)
        boxBody.transform.transform(contactWorld, contactWorld)

        manifold.contactCount = 1
        manifold.contacts[0].separation = separation
        manifold.contacts[0].position.set(contactWorld)
        return true
    }

    /**
     * Box vs Box Oriented Bounding Box (OBB) SAT (Separating Axis Theorem)
     */
    fun collideBoxBox(a: RigidBody2D, boxA: BoxShape2D, b: RigidBody2D, boxB: BoxShape2D, manifold: ContactManifold2D): Boolean {
        // Evaluate SAT on all 4 normals (2 from A, 2 from B)
        var minOverlap = Float.MAX_VALUE
        var bestNormal = Vec2()
        var flip = false

        // Check Box A Normals
        for (i in 0 until 4) {
            val normal = boxA.worldNormals[i]
            val overlap = getOverlap(boxA, boxB, normal)
            if (overlap <= 0f) return false // Found separating axis
            if (overlap < minOverlap) {
                minOverlap = overlap
                bestNormal = normal
                flip = false
            }
        }

        // Check Box B Normals
        for (i in 0 until 4) {
            val normal = boxB.worldNormals[i]
            val overlap = getOverlap(boxA, boxB, normal)
            if (overlap <= 0f) return false
            if (overlap < minOverlap) {
                minOverlap = overlap
                bestNormal = normal
                flip = true
            }
        }

        // Ensure normal points from A to B
        val centerBA = Vec2(b.transform.position.x - a.transform.position.x, b.transform.position.y - a.transform.position.y)
        if (centerBA.dot(bestNormal) < 0f) {
            bestNormal = Vec2(-bestNormal.x, -bestNormal.y)
        }

        manifold.bodyA = a
        manifold.bodyB = b
        manifold.normal.set(bestNormal)

        // Contact point generation via edge clipping
        clipContactPoints(a, boxA, b, boxB, bestNormal, minOverlap, manifold)
        return manifold.contactCount > 0
    }

    private fun getOverlap(boxA: BoxShape2D, boxB: BoxShape2D, axis: Vec2): Float {
        var minA = Float.MAX_VALUE; var maxA = -Float.MAX_VALUE
        var minB = Float.MAX_VALUE; var maxB = -Float.MAX_VALUE

        for (v in boxA.worldVertices) {
            val proj = v.dot(axis)
            minA = min(minA, proj)
            maxA = max(maxA, proj)
        }

        for (v in boxB.worldVertices) {
            val proj = v.dot(axis)
            minB = min(minB, proj)
            maxB = max(maxB, proj)
        }

        return min(maxA - minB, maxB - minA)
    }

    private fun clipContactPoints(
        a: RigidBody2D, boxA: BoxShape2D,
        b: RigidBody2D, boxB: BoxShape2D,
        normal: Vec2, penetration: Float,
        manifold: ContactManifold2D
    ) {
        var count = 0
        for (v in boxB.worldVertices) {
            val d = Vec2(v.x - a.transform.position.x, v.y - a.transform.position.y)
            if (d.dot(normal) < 0f && count < 2) {
                manifold.contacts[count].position.set(v)
                manifold.contacts[count].separation = -penetration
                count++
            }
        }

        if (count == 0) {
            // Fallback midpoint
            val midX = (a.transform.position.x + b.transform.position.x) * 0.5f
            val midY = (a.transform.position.y + b.transform.position.y) * 0.5f
            manifold.contacts[0].position.set(midX, midY)
            manifold.contacts[0].separation = -penetration
            count = 1
        }

        manifold.contactCount = count
    }

    /**
     * Dispatch general collision based on body shapes
     */
    fun checkCollision(a: RigidBody2D, b: RigidBody2D, manifold: ContactManifold2D): Boolean {
        // Fast AABB broadphase rejection
        if (!a.aabb.overlaps(b.aabb)) return false

        val sa = a.shape
        val sb = b.shape

        return when {
            sa is CircleShape2D && sb is CircleShape2D -> collideCircles(a, sa, b, sb, manifold)
            sa is CircleShape2D && sb is BoxShape2D -> collideCircleBox(a, sa, b, sb, manifold, false)
            sa is BoxShape2D && sb is CircleShape2D -> collideCircleBox(b, sb, a, sa, manifold, true)
            sa is BoxShape2D && sb is BoxShape2D -> collideBoxBox(a, sa, b, sb, manifold)
            else -> false
        }
    }
}
