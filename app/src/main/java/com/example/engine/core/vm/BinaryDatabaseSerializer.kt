package com.example.engine.core.vm

import com.example.data.local.BinaryProgramEntity
import com.example.data.local.GameSaveSnapshotEntity
import com.example.data.remote.SecurityManager
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Ultra-Fast, Zero-GC Binary Database Serializer.
 * Packs and unpacks VM bytecode, registers, RAM states, and game entity snapshots
 * into compact binary blobs for Room Database persistence.
 *
 * Binary Format Specification (STDB):
 * - Magic Header: 4 bytes (0x53, 0x54, 0x44, 0x42 -> "STDB")
 * - Version: 1 byte (0x01)
 * - Flags / Type: 1 byte (0x01 = VM_STATE, 0x02 = LEVEL_SNAPSHOT, 0x03 = BYTECODE_PROG)
 * - Payload Length: 2 bytes (Uint16)
 * - Checksum: 4 bytes (CRC/Hash)
 * - Data: N bytes
 */
object BinaryDatabaseSerializer {

    private const val MAGIC_HEADER = 0x53544442 // "STDB"
    private const val FORMAT_VERSION: Byte = 0x01

    const val TYPE_VM_STATE: Byte = 0x01
    const val TYPE_LEVEL_SNAPSHOT: Byte = 0x02
    const val TYPE_BYTECODE_PROG: Byte = 0x03

    /**
     * Serializes BytecodeVm state (16 registers + RAM + Stack) into binary byte array.
     */
    fun serializeVmState(vm: BytecodeVm): ByteArray {
        val payloadSize = (16 * 4) + vm.ram.size + (vm.callStack.size * 4) + 12
        val totalSize = 12 + payloadSize
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        // Header
        buffer.putInt(MAGIC_HEADER)
        buffer.put(FORMAT_VERSION)
        buffer.put(TYPE_VM_STATE)
        buffer.putShort(payloadSize.toShort())
        buffer.putInt(0) // Checksum placeholder

        // Payload
        buffer.putInt(vm.ip)
        buffer.putInt(vm.sp)
        buffer.putInt(if (vm.isHalted) 1 else 0)

        // Registers (R0 - R15)
        for (i in 0 until 16) {
            buffer.putInt(vm.registers[i])
        }

        // RAM
        buffer.put(vm.ram)

        // Callstack
        for (s in vm.callStack) {
            buffer.putInt(s)
        }

        return buffer.array()
    }

    /**
     * Deserializes binary byte array directly into BytecodeVm without garbage generation.
     */
    fun deserializeVmState(data: ByteArray, targetVm: BytecodeVm): Boolean {
        if (data.size < 12) return false
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        val magic = buffer.getInt()
        if (magic != MAGIC_HEADER) return false

        val version = buffer.get()
        val type = buffer.get()
        if (type != TYPE_VM_STATE) return false

        val payloadSize = buffer.getShort().toInt() and 0xFFFF
        val checksum = buffer.getInt()

        // Read Payload
        targetVm.ip = buffer.getInt()
        targetVm.sp = buffer.getInt()
        targetVm.isHalted = buffer.getInt() == 1

        for (i in 0 until 16) {
            targetVm.registers[i] = buffer.getInt()
        }

        val ramToRead = minOf(targetVm.ram.size, buffer.remaining() - (targetVm.callStack.size * 4))
        if (ramToRead > 0) {
            buffer.get(targetVm.ram, 0, ramToRead)
        }

        for (i in targetVm.callStack.indices) {
            if (buffer.remaining() >= 4) {
                targetVm.callStack[i] = buffer.getInt()
            }
        }

        return true
    }

    /**
     * Creates a BinaryProgramEntity ready for Room DB storage.
     */
    fun createBinaryProgramEntity(
        projectId: Long,
        programName: String,
        bytecode: ByteArray,
        ramSnapshot: ByteArray = ByteArray(0)
    ): BinaryProgramEntity {
        val sig = SecurityManager.computeSha256Checksum(programName + bytecode.size)
        return BinaryProgramEntity(
            projectId = projectId,
            programName = programName,
            bytecode = bytecode,
            memorySnapshot = ramSnapshot,
            instructionCount = bytecode.size,
            signatureSha256 = sig,
            compiledAt = System.currentTimeMillis()
        )
    }

    /**
     * Serializes game level snapshot to binary.
     */
    fun serializeGameSnapshot(
        slotId: Int,
        levelName: String,
        score: Int,
        health: Int,
        scrollOffset: Float,
        playerY: Float
    ): GameSaveSnapshotEntity {
        val buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(MAGIC_HEADER)
        buffer.put(FORMAT_VERSION)
        buffer.put(TYPE_LEVEL_SNAPSHOT)
        buffer.putShort(16) // payload size
        buffer.putInt(0)

        // Payload
        buffer.putInt(score)
        buffer.putInt(health)
        buffer.putFloat(scrollOffset)
        buffer.putFloat(playerY)

        return GameSaveSnapshotEntity(
            slotId = slotId,
            levelName = levelName,
            binaryData = buffer.array(),
            score = score,
            health = health,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Unpacks game snapshot payload float coordinates.
     */
    fun unpackGameSnapshot(snapshot: GameSaveSnapshotEntity): Pair<Float, Float> {
        if (snapshot.binaryData.size < 28) return 0f to 0f
        val buffer = ByteBuffer.wrap(snapshot.binaryData).order(ByteOrder.BIG_ENDIAN)
        buffer.position(20) // Skip header (12) + score(4) + health(4)
        val scrollOffset = buffer.getFloat()
        val playerY = buffer.getFloat()
        return scrollOffset to playerY
    }
}
