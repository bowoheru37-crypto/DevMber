package com.example.engine.physics

import androidx.compose.ui.graphics.Color
import com.example.engine.core.math.FastBinaryMath
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════════╗
 * ║  ⚡ PHYSICS BODY SEALED HIERARCHY (STATIC, DYNAMIC, KINEMATIC ARBITER)                    ║
 * ║  Engineered for high-efficiency mobile game engines (Android 5.0+ / Low-End ARM Chipsets)║
 * ║  Features:                                                                               ║
 * ║  - Precise Separation of Static (Terrain), Dynamic (Physics Objects), Kinematic (Platforms)║
 * ║  - Comprehensive Property Fields: Mass, Inertia, Density, Restitution, Coulomb Friction  ║
 * ║  - Bitmask Collision Layer / Mask Filtering                                              ║
 * ║  - Sleep / Wake State Machine for GC & CPU Optimization                                  ║
 * ║  - CCD (Continuous Collision Detection / Bullet Mode) Flag                               ║
 * ║  - In-Place Vector Math Integration with 0 Frame Allocations                             ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════════╝
 */
sealed class PhysicsBody(
    var id: String = "body_${System.nanoTime()}",
    var shape: PhysicsShape2D = BoxShape2D(16f, 16f),
    val bodyType: BodyType
) {
    // Spatial & Kinematic State
    val transform: Transform2D = Transform2D()
    val linearVelocity: Vec2 = Vec2()
    var angularVelocity: Float = 0f

    // Force & Torque Accumulators (Applied each step and cleared)
    val force: Vec2 = Vec2()
    var torque: Float = 0f

    // Mass & Rotational Inertia Tensor
    var density: Float = 1.0f
    var mass: Float = 1.0f
    var invMass: Float = 1.0f
    var inertia: Float = 100.0f
    var invInertia: Float = 0.01f

    // Material Physical Properties
    var restitution: Float = 0.5f       // Bounciness / Coefficient of Restitution [0.0 = inelastic, 1.0 = elastic]
    var friction: Float = 0.35f         // Coulomb Surface Friction Coefficient [0.0 = ice, 1.0 = rubber]
    var rollingFriction: Float = 0.02f  // Resistance against continuous rolling
    var linearDamping: Float = 0.996f   // Fluid / Air drag damping factor per step
    var angularDamping: Float = 0.985f  // Rotational aerodynamic damping factor per step
    var gravityScale: Float = 1.0f      // Multiplier for global world gravity (0.0 = antigrav, 1.0 = standard)

    // Spatial Bounds & Rendering
    val aabb: AABB = AABB()
    var color: Color = Color(0xFF00F0FF)
    var tag: String = "default"
    var userData: Any? = null

    // Collision Filtering & Trigger Configuration
    var collisionLayer: Int = 0x0001     // Group / Category bitmask (1, 2, 4, 8...)
    var collisionMask: Int = 0xFFFF      // Mask of layers this body collides with
    var isSensor: Boolean = false        // Sensor/Trigger: triggers callbacks without physical collision impulses
    var isBullet: Boolean = false        // CCD mode for fast projectiles to avoid tunneling

    // Rotational Lock
    var fixedRotation: Boolean = false

    // Sleep / Wake Optimization for Low-End CPUs (Unisoc / Helio / Snapdragon)
    var canSleep: Boolean = true
    var isSleeping: Boolean = false
    var sleepTimer: Float = 0f
    var sleepLinearThreshold: Float = 4.0f   // px/s
    var sleepAngularThreshold: Float = 0.08f // rad/s
    var sleepTimeRequired: Float = 0.5f      // seconds of stillness before sleeping

    init {
        updateMassData(density)
        updateAABB()
    }

    // ==========================================
    // TYPE CHECKS & QUERIES
    // ==========================================
    val isStatic: Boolean get() = this is Static
    val isDynamic: Boolean get() = this is Dynamic
    val isKinematic: Boolean get() = this is Kinematic

    // ==========================================
    // SPATIAL & TRANSFORM MODIFIERS
    // ==========================================
    fun setPosition(x: Float, y: Float): PhysicsBody {
        transform.position.set(x, y)
        updateAABB()
        wakeUp()
        return this
    }

    fun setPosition(pos: Vec2): PhysicsBody = setPosition(pos.x, pos.y)

    fun setAngle(angleRad: Float): PhysicsBody {
        transform.angle = angleRad
        transform.updateRotation()
        updateAABB()
        wakeUp()
        return this
    }

    fun setLinearVelocity(vx: Float, vy: Float): PhysicsBody {
        linearVelocity.set(vx, vy)
        if (linearVelocity.lengthSq() > 0.001f) wakeUp()
        return this
    }

    fun setAngularVelocity(w: Float): PhysicsBody {
        angularVelocity = w
        if (abs(w) > 0.001f) wakeUp()
        return this
    }

    fun updateAABB() {
        shape.updateAABB(transform, aabb)
    }

    open fun updateMassData(density: Float = this.density) {
        this.density = density
        if (bodyType == BodyType.STATIC || bodyType == BodyType.KINEMATIC) {
            mass = 0f
            invMass = 0f
            inertia = 0f
            invInertia = 0f
            return
        }

        val massInertia = FloatArray(2)
        shape.computeMassAndInertia(density, massInertia)
        mass = massInertia[0].coerceAtLeast(0.01f)
        invMass = 1.0f / mass

        if (fixedRotation) {
            inertia = 0f
            invInertia = 0f
        } else {
            inertia = massInertia[1].coerceAtLeast(1.0f)
            invInertia = 1.0f / inertia
        }
    }

    // ==========================================
    // FORCES, IMPULSES & ACCUMULATORS
    // ==========================================
    open fun applyForce(fx: Float, fy: Float) {
        if (!isDynamic) return
        force.add(fx, fy)
        wakeUp()
    }

    open fun applyForceAtPoint(fx: Float, fy: Float, worldPoint: Vec2) {
        if (!isDynamic) return
        force.add(fx, fy)
        val rx = worldPoint.x - transform.position.x
        val ry = worldPoint.y - transform.position.y
        torque += rx * fy - ry * fx
        wakeUp()
    }

    open fun applyImpulse(ix: Float, iy: Float) {
        if (!isDynamic) return
        linearVelocity.x += ix * invMass
        linearVelocity.y += iy * invMass
        wakeUp()
    }

    open fun applyImpulseAtPoint(ix: Float, iy: Float, worldPoint: Vec2) {
        if (!isDynamic) return
        linearVelocity.x += ix * invMass
        linearVelocity.y += iy * invMass
        if (!fixedRotation) {
            val rx = worldPoint.x - transform.position.x
            val ry = worldPoint.y - transform.position.y
            angularVelocity += (rx * iy - ry * ix) * invInertia
        }
        wakeUp()
    }

    open fun applyTorque(t: Float) {
        if (!isDynamic || fixedRotation) return
        torque += t
        wakeUp()
    }

    // ==========================================
    // KINEMATIC INTEGRATION
    // ==========================================
    open fun integrateForces(dt: Float, gravity: Vec2) {
        // Only Dynamic bodies respond to external forces and gravity
    }

    open fun integrateVelocities(dt: Float) {
        // Implemented by Dynamic and Kinematic bodies
    }

    // ==========================================
    // SLEEP & ACTIVITY STATE MANAGEMENT
    // ==========================================
    fun wakeUp() {
        isSleeping = false
        sleepTimer = 0f
    }

    fun putToSleep() {
        if (!canSleep || isStatic) return
        isSleeping = true
        linearVelocity.set(0f, 0f)
        angularVelocity = 0f
        force.set(0f, 0f)
        torque = 0f
    }

    fun checkSleepState(dt: Float) {
        if (!canSleep || !isDynamic) return

        val linSpeedSq = linearVelocity.lengthSq()
        val angSpeed = abs(angularVelocity)

        if (linSpeedSq < sleepLinearThreshold * sleepLinearThreshold && angSpeed < sleepAngularThreshold) {
            sleepTimer += dt
            if (sleepTimer >= sleepTimeRequired) {
                putToSleep()
            }
        } else {
            wakeUp()
        }
    }

    // ==========================================
    // COLLISION FILTERING & ENERGETICS
    // ==========================================
    fun shouldCollideWith(other: PhysicsBody): Boolean {
        if (isStatic && other.isStatic) return false
        if (isSleeping && other.isSleeping) return false
        val matchA = (collisionMask and other.collisionLayer) != 0
        val matchB = (other.collisionMask and collisionLayer) != 0
        return matchA && matchB
    }

    fun computeKineticEnergy(): Float {
        if (!isDynamic) return 0f
        val linEnergy = 0.5f * mass * linearVelocity.lengthSq()
        val rotEnergy = 0.5f * inertia * (angularVelocity * angularVelocity)
        return linEnergy + rotEnergy
    }

    fun computeLinearMomentum(out: Vec2): Vec2 {
        if (!isDynamic) return out.set(0f, 0f)
        return out.set(linearVelocity.x * mass, linearVelocity.y * mass)
    }

    // =========================================================================
    // 🏛️ SEALED SUBCLASSES: STATIC, DYNAMIC, KINEMATIC
    // =========================================================================

    /**
     * 1. STATIC BODY:
     * - Zero mass, zero velocity, infinite inertia
     * - Does not move under forces, impulses, or gravity
     * - Ideal for terrain, static platforms, barriers, bounds, walls
     */
    class Static(
        id: String = "static_${System.nanoTime()}",
        shape: PhysicsShape2D = BoxShape2D(16f, 16f)
    ) : PhysicsBody(id = id, shape = shape, bodyType = BodyType.STATIC) {
        init {
            mass = 0f
            invMass = 0f
            inertia = 0f
            invInertia = 0f
            canSleep = false
            linearVelocity.set(0f, 0f)
            angularVelocity = 0f
        }

        override fun updateMassData(density: Float) {
            this.density = density
            mass = 0f
            invMass = 0f
            inertia = 0f
            invInertia = 0f
        }

        override fun applyForce(fx: Float, fy: Float) { /* Immovable */ }
        override fun applyForceAtPoint(fx: Float, fy: Float, worldPoint: Vec2) { /* Immovable */ }
        override fun applyImpulse(ix: Float, iy: Float) { /* Immovable */ }
        override fun applyImpulseAtPoint(ix: Float, iy: Float, worldPoint: Vec2) { /* Immovable */ }
        override fun applyTorque(t: Float) { /* Immovable */ }
        override fun integrateForces(dt: Float, gravity: Vec2) { /* No integration */ }
        override fun integrateVelocities(dt: Float) { /* Static position */ }
    }

    /**
     * 2. DYNAMIC BODY:
     * - Full Newtonian mechanics, finite mass & rotational inertia
     * - Accelerates under gravity, springs, contact forces, and torque
     * - Perfect for player characters, boxes, projectiles, debris, ragdoll nodes
     */
    class Dynamic(
        id: String = "dynamic_${System.nanoTime()}",
        shape: PhysicsShape2D = BoxShape2D(16f, 16f)
    ) : PhysicsBody(id = id, shape = shape, bodyType = BodyType.DYNAMIC) {

        override fun integrateForces(dt: Float, gravity: Vec2) {
            if (isSleeping) return

            // Semi-implicit Euler integration
            val gx = gravity.x * gravityScale
            val gy = gravity.y * gravityScale

            linearVelocity.x += (force.x * invMass + gx) * dt
            linearVelocity.y += (force.y * invMass + gy) * dt
            if (!fixedRotation) {
                angularVelocity += (torque * invInertia) * dt
            }

            // Apply fluid and angular damping
            linearVelocity.scale(linearDamping)
            angularVelocity *= angularDamping

            // Reset accumulators
            force.set(0f, 0f)
            torque = 0f

            // Float NaN & Infinity sanitization
            linearVelocity.sanitize()
            if (angularVelocity.isNaN() || angularVelocity.isInfinite()) angularVelocity = 0f
        }

        override fun integrateVelocities(dt: Float) {
            if (isSleeping) return

            transform.position.x += linearVelocity.x * dt
            transform.position.y += linearVelocity.y * dt
            if (!fixedRotation) {
                transform.angle += angularVelocity * dt
            }

            transform.position.sanitize()
            if (transform.angle.isNaN() || transform.angle.isInfinite()) transform.angle = 0f

            transform.updateRotation()
            updateAABB()

            // Update sleep evaluation
            checkSleepState(dt)
        }
    }

    /**
     * 3. KINEMATIC BODY:
     * - Script-driven or velocity-driven motion (e.g. moving elevator, conveyor, oscillating blade)
     * - Infinite mass (invMass = 0, invInertia = 0), pushes Dynamic bodies without being pushed back
     * - Ignores gravity and external contact forces, but moves strictly based on its linearVelocity and angularVelocity
     */
    class Kinematic(
        id: String = "kinematic_${System.nanoTime()}",
        shape: PhysicsShape2D = BoxShape2D(16f, 16f)
    ) : PhysicsBody(id = id, shape = shape, bodyType = BodyType.KINEMATIC) {
        init {
            mass = 0f
            invMass = 0f
            inertia = 0f
            invInertia = 0f
            canSleep = false
        }

        override fun updateMassData(density: Float) {
            this.density = density
            mass = 0f
            invMass = 0f
            inertia = 0f
            invInertia = 0f
        }

        override fun applyForce(fx: Float, fy: Float) { /* Kinematic ignores forces */ }
        override fun applyForceAtPoint(fx: Float, fy: Float, worldPoint: Vec2) { /* Kinematic ignores forces */ }
        override fun applyImpulse(ix: Float, iy: Float) { /* Kinematic ignores impulses */ }
        override fun applyImpulseAtPoint(ix: Float, iy: Float, worldPoint: Vec2) { /* Kinematic ignores impulses */ }
        override fun applyTorque(t: Float) { /* Kinematic ignores torque */ }

        override fun integrateForces(dt: Float, gravity: Vec2) {
            // Kinematic bodies do NOT respond to gravity or forces
        }

        override fun integrateVelocities(dt: Float) {
            transform.position.x += linearVelocity.x * dt
            transform.position.y += linearVelocity.y * dt
            transform.angle += angularVelocity * dt

            transform.position.sanitize()
            if (transform.angle.isNaN() || transform.angle.isInfinite()) transform.angle = 0f

            transform.updateRotation()
            updateAABB()
        }
    }

    companion object {
        fun createStatic(
            shape: PhysicsShape2D,
            x: Float = 0f,
            y: Float = 0f,
            color: Color = Color(0xFF1E293B)
        ): Static {
            val body = Static(shape = shape)
            body.setPosition(x, y)
            body.color = color
            return body
        }

        fun createDynamic(
            shape: PhysicsShape2D,
            x: Float = 0f,
            y: Float = 0f,
            color: Color = Color(0xFF00F0FF),
            restitution: Float = 0.5f,
            friction: Float = 0.35f,
            density: Float = 1.0f
        ): Dynamic {
            val body = Dynamic(shape = shape)
            body.setPosition(x, y)
            body.color = color
            body.restitution = restitution
            body.friction = friction
            body.updateMassData(density)
            return body
        }

        fun createKinematic(
            shape: PhysicsShape2D,
            x: Float = 0f,
            y: Float = 0f,
            vx: Float = 0f,
            vy: Float = 0f,
            color: Color = Color(0xFF9D4EDD)
        ): Kinematic {
            val body = Kinematic(shape = shape)
            body.setPosition(x, y)
            body.setLinearVelocity(vx, vy)
            body.color = color
            return body
        }
    }
}
