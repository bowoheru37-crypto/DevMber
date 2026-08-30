package com.example.engine.physics

import kotlin.math.sqrt

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════════╗
 * ║  ⚡ PHYSICS JOINTS & CONSTRAINTS ENGINE (DISTANCE, REVOLUTE, SPRING, MOUSE)                ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════════╝
 */
sealed class PhysicsJoint2D(
    val bodyA: RigidBody2D,
    val bodyB: RigidBody2D
) {
    var isEnabled: Boolean = true
    abstract fun preStep(dt: Float, invDt: Float)
    abstract fun solveVelocity()
}

/**
 * Distance Joint (Rigid Rod or Elastic Spring)
 */
class DistanceJoint2D(
    bodyA: RigidBody2D,
    bodyB: RigidBody2D,
    val localAnchorA: Vec2 = Vec2(),
    val localAnchorB: Vec2 = Vec2(),
    var targetDistance: Float = 60f,
    var frequencyHz: Float = 0f, // 0 = rigid rod, > 0 = spring
    var dampingRatio: Float = 0.7f
) : PhysicsJoint2D(bodyA, bodyB) {

    private val rA = Vec2()
    private val rB = Vec2()
    private val u = Vec2()
    private var impulse = 0f
    private var effectiveMass = 0f
    private var bias = 0f
    private var gamma = 0f

    override fun preStep(dt: Float, invDt: Float) {
        bodyA.transform.rotation.mul(localAnchorA, rA)
        bodyB.transform.rotation.mul(localAnchorB, rB)

        val pAx = bodyA.transform.position.x + rA.x
        val pAy = bodyA.transform.position.y + rA.y
        val pBx = bodyB.transform.position.x + rB.x
        val pBy = bodyB.transform.position.y + rB.y

        val dx = pBx - pAx
        val dy = pBy - pAy
        val currentDist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
        u.set(dx / currentDist, dy / currentDist)

        val crA = rA.cross(u)
        val crB = rB.cross(u)
        val invMass = bodyA.invMass + bodyB.invMass + (crA * crA) * bodyA.invInertia + (crB * crB) * bodyB.invInertia

        if (frequencyHz > 0f) {
            val omega = 2f * Math.PI.toFloat() * frequencyHz
            val d = 2f * invMass * dampingRatio * omega
            val k = invMass * omega * omega
            gamma = 1.0f / (dt * (d + dt * k))
            bias = (currentDist - targetDistance) * (dt * k * gamma)
            effectiveMass = 1.0f / (invMass + gamma)
        } else {
            gamma = 0f
            bias = 0.2f * invDt * (currentDist - targetDistance)
            effectiveMass = if (invMass > 0f) 1.0f / invMass else 0f
        }

        // Apply cached impulse
        val px = u.x * impulse
        val py = u.y * impulse
        bodyA.linearVelocity.x -= px * bodyA.invMass
        bodyA.linearVelocity.y -= py * bodyA.invMass
        bodyA.angularVelocity -= rA.cross(px, py) * bodyA.invInertia

        bodyB.linearVelocity.x += px * bodyB.invMass
        bodyB.linearVelocity.y += py * bodyB.invMass
        bodyB.angularVelocity += rB.cross(px, py) * bodyB.invInertia
    }

    override fun solveVelocity() {
        val vAx = bodyA.linearVelocity.x - bodyA.angularVelocity * rA.y
        val vAy = bodyA.linearVelocity.y + bodyA.angularVelocity * rA.x
        val vBx = bodyB.linearVelocity.x - bodyB.angularVelocity * rB.y
        val vBy = bodyB.linearVelocity.y + bodyB.angularVelocity * rB.x

        val cDot = (vBx - vAx) * u.x + (vBy - vAy) * u.y
        val deltaImpulse = -effectiveMass * (cDot + bias + gamma * impulse)
        impulse += deltaImpulse

        val px = u.x * deltaImpulse
        val py = u.y * deltaImpulse
        bodyA.linearVelocity.x -= px * bodyA.invMass
        bodyA.linearVelocity.y -= py * bodyA.invMass
        bodyA.angularVelocity -= rA.cross(px, py) * bodyA.invInertia

        bodyB.linearVelocity.x += px * bodyB.invMass
        bodyB.linearVelocity.y += py * bodyB.invMass
        bodyB.angularVelocity += rB.cross(px, py) * bodyB.invInertia
    }
}

/**
 * Revolute / Pin Joint (Constrains two anchor points to coincide in world space)
 */
class RevoluteJoint2D(
    bodyA: RigidBody2D,
    bodyB: RigidBody2D,
    val localAnchorA: Vec2 = Vec2(),
    val localAnchorB: Vec2 = Vec2()
) : PhysicsJoint2D(bodyA, bodyB) {

    private val rA = Vec2()
    private val rB = Vec2()
    private val impulse = Vec2()
    private val bias = Vec2()
    private var k00 = 0f; private var k01 = 0f
    private var k10 = 0f; private var k11 = 0f

    override fun preStep(dt: Float, invDt: Float) {
        bodyA.transform.rotation.mul(localAnchorA, rA)
        bodyB.transform.rotation.mul(localAnchorB, rB)

        val ma = bodyA.invMass; val mb = bodyB.invMass
        val ia = bodyA.invInertia; val ib = bodyB.invInertia

        k00 = ma + mb + rA.y * rA.y * ia + rB.y * rB.y * ib
        k01 = -rA.y * rA.x * ia - rB.y * rB.x * ib
        k10 = k01
        k11 = ma + mb + rA.x * rA.x * ia + rB.x * rB.x * ib

        val pAx = bodyA.transform.position.x + rA.x
        val pAy = bodyA.transform.position.y + rA.y
        val pBx = bodyB.transform.position.x + rB.x
        val pBy = bodyB.transform.position.y + rB.y

        bias.set(0.2f * invDt * (pBx - pAx), 0.2f * invDt * (pBy - pAy))

        // Warm starting
        bodyA.linearVelocity.x -= impulse.x * ma
        bodyA.linearVelocity.y -= impulse.y * ma
        bodyA.angularVelocity -= rA.cross(impulse) * ia

        bodyB.linearVelocity.x += impulse.x * mb
        bodyB.linearVelocity.y += impulse.y * mb
        bodyB.angularVelocity += rB.cross(impulse) * ib
    }

    override fun solveVelocity() {
        val vAx = bodyA.linearVelocity.x - bodyA.angularVelocity * rA.y
        val vAy = bodyA.linearVelocity.y + bodyA.angularVelocity * rA.x
        val vBx = bodyB.linearVelocity.x - bodyB.angularVelocity * rB.y
        val vBy = bodyB.linearVelocity.y + bodyB.angularVelocity * rB.x

        val cDotX = (vBx - vAx) + bias.x
        val cDotY = (vBy - vAy) + bias.y

        // Invert 2x2 K matrix
        val det = k00 * k11 - k01 * k10
        val invDet = if (det != 0f) 1.0f / det else 0f
        val deltaX = -invDet * (k11 * cDotX - k01 * cDotY)
        val deltaY = -invDet * (-k10 * cDotX + k00 * cDotY)

        impulse.x += deltaX
        impulse.y += deltaY

        val ma = bodyA.invMass; val mb = bodyB.invMass
        val ia = bodyA.invInertia; val ib = bodyB.invInertia

        bodyA.linearVelocity.x -= deltaX * ma
        bodyA.linearVelocity.y -= deltaY * ma
        bodyA.angularVelocity -= (rA.x * deltaY - rA.y * deltaX) * ia

        bodyB.linearVelocity.x += deltaX * mb
        bodyB.linearVelocity.y += deltaY * mb
        bodyB.angularVelocity += (rB.x * deltaY - rB.y * deltaX) * ib
    }
}

/**
 * Mouse & Touch Spring Joint (Allows interactive user dragging without blowing up velocities)
 */
class MouseJoint2D(
    val body: RigidBody2D,
    val localAnchor: Vec2 = Vec2()
) {
    val target: Vec2 = Vec2()
    var maxForce: Float = 2000f
    var frequencyHz: Float = 5.0f
    var dampingRatio: Float = 0.7f

    private val r = Vec2()
    private val impulse = Vec2()
    private val bias = Vec2()
    private var gamma = 0f
    private var k00 = 0f; private var k01 = 0f
    private var k10 = 0f; private var k11 = 0f

    fun preStep(dt: Float, invDt: Float) {
        body.transform.rotation.mul(localAnchor, r)

        val m = body.invMass
        val i = body.invInertia

        val omega = 2f * Math.PI.toFloat() * frequencyHz
        val d = 2f * body.mass * dampingRatio * omega
        val k = body.mass * omega * omega

        gamma = 1.0f / (dt * (d + dt * k))
        val beta = dt * k * gamma

        val px = body.transform.position.x + r.x
        val py = body.transform.position.y + r.y
        bias.set((px - target.x) * beta * invDt, (py - target.y) * beta * invDt)

        k00 = m + r.y * r.y * i + gamma
        k01 = -r.y * r.x * i
        k10 = k01
        k11 = m + r.x * r.x * i + gamma

        body.linearVelocity.x += impulse.x * m
        body.linearVelocity.y += impulse.y * m
        body.angularVelocity += r.cross(impulse) * i
    }

    fun solveVelocity(dt: Float) {
        val vx = body.linearVelocity.x - body.angularVelocity * r.y
        val vy = body.linearVelocity.y + body.angularVelocity * r.x

        val cDotX = vx + bias.x + gamma * impulse.x
        val cDotY = vy + bias.y + gamma * impulse.y

        val det = k00 * k11 - k01 * k10
        val invDet = if (det != 0f) 1.0f / det else 0f
        var deltaX = -invDet * (k11 * cDotX - k01 * cDotY)
        var deltaY = -invDet * (-k10 * cDotX + k00 * cDotY)

        val oldImpulseX = impulse.x
        val oldImpulseY = impulse.y
        impulse.x += deltaX
        impulse.y += deltaY

        val maxImp = maxForce * dt
        val impLenSq = impulse.lengthSq()
        if (impLenSq > maxImp * maxImp) {
            impulse.scale(maxImp / sqrt(impLenSq))
        }
        deltaX = impulse.x - oldImpulseX
        deltaY = impulse.y - oldImpulseY

        val m = body.invMass
        val i = body.invInertia
        body.linearVelocity.x += deltaX * m
        body.linearVelocity.y += deltaY * m
        body.angularVelocity += (r.x * deltaY - r.y * deltaX) * i
    }
}
