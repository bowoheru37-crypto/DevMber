package com.example.engine.core.vm

import com.example.engine.AudioSynthesizer

/**
 * 8-Bit Binary Opcode Instruction Set.
 * Provides custom binary machine code execution for game logic, entity behavior scripts,
 * movement routines, and algorithmic procedural synthesis.
 */
object Opcodes {
    const val NOP: Byte = 0x00
    const val LOAD_CONST: Byte = 0x01   // LOAD_CONST Reg, Val32 (5 bytes)
    const val MOV: Byte = 0x02          // MOV RegDst, RegSrc (3 bytes)
    const val STORE_MEM: Byte = 0x03    // STORE_MEM Addr16, Reg (4 bytes)
    const val LOAD_MEM: Byte = 0x04     // LOAD_MEM Reg, Addr16 (4 bytes)

    // Arithmetic & Bitwise
    const val ADD: Byte = 0x10          // ADD RegDst, RegSrc
    const val SUB: Byte = 0x11          // SUB RegDst, RegSrc
    const val MUL: Byte = 0x12          // MUL RegDst, RegSrc
    const val DIV: Byte = 0x13          // DIV RegDst, RegSrc
    const val AND: Byte = 0x14          // AND RegDst, RegSrc
    const val OR: Byte = 0x15           // OR RegDst, RegSrc
    const val XOR: Byte = 0x16          // XOR RegDst, RegSrc
    const val SHL: Byte = 0x17          // SHL RegDst, Imm8
    const val SHR: Byte = 0x18          // SHR RegDst, Imm8
    const val INC: Byte = 0x19          // INC RegDst
    const val DEC: Byte = 0x1A          // DEC RegDst

    // Branching & Control Flow
    const val JMP: Byte = 0x20          // JMP Addr16
    const val JZ: Byte = 0x21           // JZ Reg, Addr16
    const val JNZ: Byte = 0x22          // JNZ Reg, Addr16
    const val JGT: Byte = 0x23          // JGT RegA, RegB, Addr16
    const val JLT: Byte = 0x24          // JLT RegA, RegB, Addr16
    const val CALL: Byte = 0x25         // CALL Addr16
    const val RET: Byte = 0x26          // RET

    // Game Engine Hardware I/O Interop
    const val SET_ENTITY_POS: Byte = 0x30  // SET_ENTITY_POS RegX, RegY
    const val GET_ENTITY_POS: Byte = 0x31  // GET_ENTITY_POS RegX, RegY
    const val PLAY_SFX: Byte = 0x32        // PLAY_SFX RegFreq, RegDur
    const val SPAWN_PARTICLE: Byte = 0x33  // SPAWN_PARTICLE RegType

    // Binary & Database Storage Interop
    const val DB_SAVE_STATE: Byte = 0x34   // DB_SAVE RegAddr16
    const val DB_LOAD_STATE: Byte = 0x35   // DB_LOAD RegAddr16
    const val DB_LOOKUP: Byte = 0x36       // DB_LOOKUP RegDst, RegKey, RegTable

    // System
    const val HALT: Byte = 0xFF.toByte()
}

/**
 * High-speed, Zero-Allocation Bytecode Virtual Machine with 16 32-bit registers (R0-R15),
 * 1024-byte contiguous memory RAM, call stack, and cycle limiter.
 */
class BytecodeVm(val ramSize: Int = 1024) {
    val registers = IntArray(16) { 0 }
    val ram = ByteArray(ramSize) { 0 }
    val callStack = IntArray(32) { 0 }
    var sp: Int = 0  // Stack Pointer
    var ip: Int = 0  // Instruction Pointer
    var isHalted: Boolean = false
    var totalCycles: Long = 0

    // Hardware Output State (Engine Interop)
    var entityPosX: Float = 100f
    var entityPosY: Float = 150f
    var lastSfxFreq: Float = 0f
    var particleEmitCount: Int = 0

    fun reset() {
        registers.fill(0)
        ram.fill(0)
        callStack.fill(0)
        sp = 0
        ip = 0
        isHalted = false
        totalCycles = 0
    }

    fun loadBytecode(bytecode: ByteArray, offset: Int = 0) {
        reset()
        val copyLen = minOf(bytecode.size, ram.size - offset)
        System.arraycopy(bytecode, 0, ram, offset, copyLen)
    }

    /**
     * Executes up to `maxCycles` instructions. Returns true if still running, false if halted.
     */
    fun step(maxCycles: Int = 100): Boolean {
        if (isHalted) return false
        var cycles = 0

        while (cycles < maxCycles && !isHalted && ip < ram.size) {
            val opcode = ram[ip++]
            cycles++
            totalCycles++

            when (opcode) {
                Opcodes.NOP -> Unit
                Opcodes.LOAD_CONST -> {
                    val reg = ram[ip++].toInt() and 0x0F
                    val val32 = readInt32(ip)
                    ip += 4
                    registers[reg] = val32
                }
                Opcodes.MOV -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    val src = ram[ip++].toInt() and 0x0F
                    registers[dst] = registers[src]
                }
                Opcodes.STORE_MEM -> {
                    val addr = readInt16(ip)
                    ip += 2
                    val reg = ram[ip++].toInt() and 0x0F
                    if (addr in 0..ram.size - 4) {
                        writeInt32(addr, registers[reg])
                    }
                }
                Opcodes.LOAD_MEM -> {
                    val reg = ram[ip++].toInt() and 0x0F
                    val addr = readInt16(ip)
                    ip += 2
                    if (addr in 0..ram.size - 4) {
                        registers[reg] = readInt32(addr)
                    }
                }
                Opcodes.ADD -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    val src = ram[ip++].toInt() and 0x0F
                    registers[dst] += registers[src]
                }
                Opcodes.SUB -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    val src = ram[ip++].toInt() and 0x0F
                    registers[dst] -= registers[src]
                }
                Opcodes.MUL -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    val src = ram[ip++].toInt() and 0x0F
                    registers[dst] *= registers[src]
                }
                Opcodes.DIV -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    val src = ram[ip++].toInt() and 0x0F
                    if (registers[src] != 0) {
                        registers[dst] /= registers[src]
                    }
                }
                Opcodes.AND -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    val src = ram[ip++].toInt() and 0x0F
                    registers[dst] = registers[dst] and registers[src]
                }
                Opcodes.OR -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    val src = ram[ip++].toInt() and 0x0F
                    registers[dst] = registers[dst] or registers[src]
                }
                Opcodes.XOR -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    val src = ram[ip++].toInt() and 0x0F
                    registers[dst] = registers[dst] xor registers[src]
                }
                Opcodes.SHL -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    val shift = ram[ip++].toInt() and 0x1F
                    registers[dst] = registers[dst] shl shift
                }
                Opcodes.SHR -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    val shift = ram[ip++].toInt() and 0x1F
                    registers[dst] = registers[dst] ushr shift
                }
                Opcodes.INC -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    registers[dst]++
                }
                Opcodes.DEC -> {
                    val dst = ram[ip++].toInt() and 0x0F
                    registers[dst]--
                }
                Opcodes.JMP -> {
                    val addr = readInt16(ip)
                    ip = addr.coerceIn(0, ram.size - 1)
                }
                Opcodes.JZ -> {
                    val reg = ram[ip++].toInt() and 0x0F
                    val addr = readInt16(ip)
                    ip += 2
                    if (registers[reg] == 0) {
                        ip = addr.coerceIn(0, ram.size - 1)
                    }
                }
                Opcodes.JNZ -> {
                    val reg = ram[ip++].toInt() and 0x0F
                    val addr = readInt16(ip)
                    ip += 2
                    if (registers[reg] != 0) {
                        ip = addr.coerceIn(0, ram.size - 1)
                    }
                }
                Opcodes.JGT -> {
                    val rA = ram[ip++].toInt() and 0x0F
                    val rB = ram[ip++].toInt() and 0x0F
                    val addr = readInt16(ip)
                    ip += 2
                    if (registers[rA] > registers[rB]) {
                        ip = addr.coerceIn(0, ram.size - 1)
                    }
                }
                Opcodes.JLT -> {
                    val rA = ram[ip++].toInt() and 0x0F
                    val rB = ram[ip++].toInt() and 0x0F
                    val addr = readInt16(ip)
                    ip += 2
                    if (registers[rA] < registers[rB]) {
                        ip = addr.coerceIn(0, ram.size - 1)
                    }
                }
                Opcodes.CALL -> {
                    val addr = readInt16(ip)
                    ip += 2
                    if (sp < callStack.size) {
                        callStack[sp++] = ip
                        ip = addr.coerceIn(0, ram.size - 1)
                    }
                }
                Opcodes.RET -> {
                    if (sp > 0) {
                        ip = callStack[--sp]
                    } else {
                        isHalted = true
                    }
                }
                Opcodes.SET_ENTITY_POS -> {
                    val rX = ram[ip++].toInt() and 0x0F
                    val rY = ram[ip++].toInt() and 0x0F
                    entityPosX = registers[rX].toFloat()
                    entityPosY = registers[rY].toFloat()
                }
                Opcodes.GET_ENTITY_POS -> {
                    val rX = ram[ip++].toInt() and 0x0F
                    val rY = ram[ip++].toInt() and 0x0F
                    registers[rX] = entityPosX.toInt()
                    registers[rY] = entityPosY.toInt()
                }
                Opcodes.PLAY_SFX -> {
                    val rFreq = ram[ip++].toInt() and 0x0F
                    val rDur = ram[ip++].toInt() and 0x0F
                    val freq = registers[rFreq].toFloat().coerceIn(60f, 4000f)
                    val dur = registers[rDur].coerceIn(20, 300)
                    lastSfxFreq = freq
                    AudioSynthesizer.playToneAsync(freq, durationMs = dur, volume = 0.25f)
                }
                Opcodes.SPAWN_PARTICLE -> {
                    val rType = ram[ip++].toInt() and 0x0F
                    particleEmitCount += registers[rType].coerceIn(1, 10)
                }
                Opcodes.DB_SAVE_STATE -> {
                    val rAddr = ram[ip++].toInt() and 0x0F
                    val baseAddr = registers[rAddr].coerceIn(0, ram.size - (16 * 4))
                    for (r in 0 until 16) {
                        writeInt32(baseAddr + (r * 4), registers[r])
                    }
                }
                Opcodes.DB_LOAD_STATE -> {
                    val rAddr = ram[ip++].toInt() and 0x0F
                    val baseAddr = registers[rAddr].coerceIn(0, ram.size - (16 * 4))
                    for (r in 0 until 16) {
                        registers[r] = readInt32(baseAddr + (r * 4))
                    }
                }
                Opcodes.DB_LOOKUP -> {
                    val rDst = ram[ip++].toInt() and 0x0F
                    val rKey = ram[ip++].toInt() and 0x0F
                    val rTable = ram[ip++].toInt() and 0x0F
                    val tableBase = registers[rTable].coerceIn(0, ram.size - 8)
                    val keyVal = registers[rKey]
                    // Fast binary indexed search in 8-byte structured records (Key32 + Val32)
                    var found = 0
                    var offset = tableBase
                    while (offset <= ram.size - 8) {
                        val recordKey = readInt32(offset)
                        if (recordKey == keyVal) {
                            found = readInt32(offset + 4)
                            break
                        } else if (recordKey == 0 && readInt32(offset + 4) == 0) {
                            break // End of table marker
                        }
                        offset += 8
                    }
                    registers[rDst] = found
                }
                Opcodes.HALT -> {
                    isHalted = true
                }
                else -> {
                    // Unknown opcode -> halt for safety
                    isHalted = true
                }
            }
        }
        return !isHalted
    }

    private fun readInt16(offset: Int): Int {
        if (offset + 1 >= ram.size) return 0
        return ((ram[offset].toInt() and 0xFF) shl 8) or (ram[offset + 1].toInt() and 0xFF)
    }

    private fun readInt32(offset: Int): Int {
        if (offset + 3 >= ram.size) return 0
        return ((ram[offset].toInt() and 0xFF) shl 24) or
                ((ram[offset + 1].toInt() and 0xFF) shl 16) or
                ((ram[offset + 2].toInt() and 0xFF) shl 8) or
                (ram[offset + 3].toInt() and 0xFF)
    }

    private fun writeInt32(offset: Int, value: Int) {
        if (offset + 3 >= ram.size) return
        ram[offset] = ((value shr 24) and 0xFF).toByte()
        ram[offset + 1] = ((value shr 16) and 0xFF).toByte()
        ram[offset + 2] = ((value shr 8) and 0xFF).toByte()
        ram[offset + 3] = (value and 0xFF).toByte()
    }
}

/**
 * Text Assembler and Binary Disassembler.
 * Translates human-readable assembly instructions to binary byte arrays and vice-versa.
 */
object AssemblerEngine {

    fun assemble(source: String): ByteArray {
        val bytes = mutableListOf<Byte>()
        val lines = source.lines()

        for (line in lines) {
            val clean = line.split("//")[0].split(";")[0].trim()
            if (clean.isEmpty()) continue

            val tokens = clean.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
            if (tokens.isEmpty()) continue

            val mnemonic = tokens[0].uppercase()
            when (mnemonic) {
                "NOP" -> bytes.add(Opcodes.NOP)
                "LOAD_CONST", "SET" -> {
                    val reg = parseReg(tokens.getOrNull(1))
                    val value = tokens.getOrNull(2)?.toIntOrNull() ?: 0
                    bytes.add(Opcodes.LOAD_CONST)
                    bytes.add(reg)
                    appendInt32(bytes, value)
                }
                "MOV" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    val src = parseReg(tokens.getOrNull(2))
                    bytes.add(Opcodes.MOV)
                    bytes.add(dst)
                    bytes.add(src)
                }
                "ADD" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    val src = parseReg(tokens.getOrNull(2))
                    bytes.add(Opcodes.ADD)
                    bytes.add(dst)
                    bytes.add(src)
                }
                "SUB" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    val src = parseReg(tokens.getOrNull(2))
                    bytes.add(Opcodes.SUB)
                    bytes.add(dst)
                    bytes.add(src)
                }
                "MUL" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    val src = parseReg(tokens.getOrNull(2))
                    bytes.add(Opcodes.MUL)
                    bytes.add(dst)
                    bytes.add(src)
                }
                "DIV" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    val src = parseReg(tokens.getOrNull(2))
                    bytes.add(Opcodes.DIV)
                    bytes.add(dst)
                    bytes.add(src)
                }
                "AND" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    val src = parseReg(tokens.getOrNull(2))
                    bytes.add(Opcodes.AND)
                    bytes.add(dst)
                    bytes.add(src)
                }
                "OR" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    val src = parseReg(tokens.getOrNull(2))
                    bytes.add(Opcodes.OR)
                    bytes.add(dst)
                    bytes.add(src)
                }
                "XOR" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    val src = parseReg(tokens.getOrNull(2))
                    bytes.add(Opcodes.XOR)
                    bytes.add(dst)
                    bytes.add(src)
                }
                "SHL" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    val shift = (tokens.getOrNull(2)?.toIntOrNull() ?: 1).toByte()
                    bytes.add(Opcodes.SHL)
                    bytes.add(dst)
                    bytes.add(shift)
                }
                "SHR" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    val shift = (tokens.getOrNull(2)?.toIntOrNull() ?: 1).toByte()
                    bytes.add(Opcodes.SHR)
                    bytes.add(dst)
                    bytes.add(shift)
                }
                "INC" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    bytes.add(Opcodes.INC)
                    bytes.add(dst)
                }
                "DEC" -> {
                    val dst = parseReg(tokens.getOrNull(1))
                    bytes.add(Opcodes.DEC)
                    bytes.add(dst)
                }
                "JMP" -> {
                    val addr = tokens.getOrNull(1)?.toIntOrNull() ?: 0
                    bytes.add(Opcodes.JMP)
                    appendInt16(bytes, addr)
                }
                "JZ" -> {
                    val reg = parseReg(tokens.getOrNull(1))
                    val addr = tokens.getOrNull(2)?.toIntOrNull() ?: 0
                    bytes.add(Opcodes.JZ)
                    bytes.add(reg)
                    appendInt16(bytes, addr)
                }
                "JNZ" -> {
                    val reg = parseReg(tokens.getOrNull(1))
                    val addr = tokens.getOrNull(2)?.toIntOrNull() ?: 0
                    bytes.add(Opcodes.JNZ)
                    bytes.add(reg)
                    appendInt16(bytes, addr)
                }
                "SET_POS" -> {
                    val rX = parseReg(tokens.getOrNull(1))
                    val rY = parseReg(tokens.getOrNull(2))
                    bytes.add(Opcodes.SET_ENTITY_POS)
                    bytes.add(rX)
                    bytes.add(rY)
                }
                "GET_POS" -> {
                    val rX = parseReg(tokens.getOrNull(1))
                    val rY = parseReg(tokens.getOrNull(2))
                    bytes.add(Opcodes.GET_ENTITY_POS)
                    bytes.add(rX)
                    bytes.add(rY)
                }
                "PLAY_SFX" -> {
                    val rFreq = parseReg(tokens.getOrNull(1))
                    val rDur = parseReg(tokens.getOrNull(2))
                    bytes.add(Opcodes.PLAY_SFX)
                    bytes.add(rFreq)
                    bytes.add(rDur)
                }
                "SPAWN_PARTICLE" -> {
                    val rType = parseReg(tokens.getOrNull(1))
                    bytes.add(Opcodes.SPAWN_PARTICLE)
                    bytes.add(rType)
                }
                "DB_SAVE" -> {
                    val rAddr = parseReg(tokens.getOrNull(1))
                    bytes.add(Opcodes.DB_SAVE_STATE)
                    bytes.add(rAddr)
                }
                "DB_LOAD" -> {
                    val rAddr = parseReg(tokens.getOrNull(1))
                    bytes.add(Opcodes.DB_LOAD_STATE)
                    bytes.add(rAddr)
                }
                "DB_LOOKUP" -> {
                    val rDst = parseReg(tokens.getOrNull(1))
                    val rKey = parseReg(tokens.getOrNull(2))
                    val rTable = parseReg(tokens.getOrNull(3))
                    bytes.add(Opcodes.DB_LOOKUP)
                    bytes.add(rDst)
                    bytes.add(rKey)
                    bytes.add(rTable)
                }
                "HALT" -> bytes.add(Opcodes.HALT)
            }
        }

        if (bytes.isEmpty() || bytes.last() != Opcodes.HALT) {
            bytes.add(Opcodes.HALT)
        }

        return bytes.toByteArray()
    }

    fun disassemble(bytes: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val addr = String.format("%04X", i)
            val op = bytes[i++]
            when (op) {
                Opcodes.NOP -> sb.appendLine("$addr: NOP")
                Opcodes.LOAD_CONST -> {
                    if (i + 4 < bytes.size) {
                        val reg = bytes[i++].toInt() and 0x0F
                        val val32 = ((bytes[i++].toInt() and 0xFF) shl 24) or
                                ((bytes[i++].toInt() and 0xFF) shl 16) or
                                ((bytes[i++].toInt() and 0xFF) shl 8) or
                                (bytes[i++].toInt() and 0xFF)
                        sb.appendLine("$addr: LOAD_CONST R$reg, $val32")
                    }
                }
                Opcodes.MOV -> {
                    if (i + 1 < bytes.size) {
                        val dst = bytes[i++].toInt() and 0x0F
                        val src = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: MOV R$dst, R$src")
                    }
                }
                Opcodes.ADD -> {
                    if (i + 1 < bytes.size) {
                        val dst = bytes[i++].toInt() and 0x0F
                        val src = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: ADD R$dst, R$src")
                    }
                }
                Opcodes.SUB -> {
                    if (i + 1 < bytes.size) {
                        val dst = bytes[i++].toInt() and 0x0F
                        val src = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: SUB R$dst, R$src")
                    }
                }
                Opcodes.MUL -> {
                    if (i + 1 < bytes.size) {
                        val dst = bytes[i++].toInt() and 0x0F
                        val src = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: MUL R$dst, R$src")
                    }
                }
                Opcodes.DIV -> {
                    if (i + 1 < bytes.size) {
                        val dst = bytes[i++].toInt() and 0x0F
                        val src = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: DIV R$dst, R$src")
                    }
                }
                Opcodes.AND -> {
                    if (i + 1 < bytes.size) {
                        val dst = bytes[i++].toInt() and 0x0F
                        val src = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: AND R$dst, R$src")
                    }
                }
                Opcodes.OR -> {
                    if (i + 1 < bytes.size) {
                        val dst = bytes[i++].toInt() and 0x0F
                        val src = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: OR R$dst, R$src")
                    }
                }
                Opcodes.XOR -> {
                    if (i + 1 < bytes.size) {
                        val dst = bytes[i++].toInt() and 0x0F
                        val src = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: XOR R$dst, R$src")
                    }
                }
                Opcodes.INC -> {
                    if (i < bytes.size) {
                        val dst = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: INC R$dst")
                    }
                }
                Opcodes.DEC -> {
                    if (i < bytes.size) {
                        val dst = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: DEC R$dst")
                    }
                }
                Opcodes.SET_ENTITY_POS -> {
                    if (i + 1 < bytes.size) {
                        val rX = bytes[i++].toInt() and 0x0F
                        val rY = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: SET_POS R$rX, R$rY")
                    }
                }
                Opcodes.PLAY_SFX -> {
                    if (i + 1 < bytes.size) {
                        val rF = bytes[i++].toInt() and 0x0F
                        val rD = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: PLAY_SFX R$rF, R$rD")
                    }
                }
                Opcodes.DB_SAVE_STATE -> {
                    if (i < bytes.size) {
                        val rA = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: DB_SAVE R$rA")
                    }
                }
                Opcodes.DB_LOAD_STATE -> {
                    if (i < bytes.size) {
                        val rA = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: DB_LOAD R$rA")
                    }
                }
                Opcodes.DB_LOOKUP -> {
                    if (i + 2 < bytes.size) {
                        val rDst = bytes[i++].toInt() and 0x0F
                        val rKey = bytes[i++].toInt() and 0x0F
                        val rTbl = bytes[i++].toInt() and 0x0F
                        sb.appendLine("$addr: DB_LOOKUP R$rDst, R$rKey, R$rTbl")
                    }
                }
                Opcodes.HALT -> sb.appendLine("$addr: HALT")
                else -> sb.appendLine("$addr: DB 0x${String.format("%02X", op)}")
            }
        }
        return sb.toString()
    }

    private fun parseReg(token: String?): Byte {
        if (token == null) return 0
        val t = token.uppercase().removePrefix("R")
        return (t.toIntOrNull() ?: 0).coerceIn(0, 15).toByte()
    }

    private fun appendInt16(list: MutableList<Byte>, value: Int) {
        list.add(((value shr 8) and 0xFF).toByte())
        list.add((value and 0xFF).toByte())
    }

    private fun appendInt32(list: MutableList<Byte>, value: Int) {
        list.add(((value shr 24) and 0xFF).toByte())
        list.add(((value shr 16) and 0xFF).toByte())
        list.add(((value shr 8) and 0xFF).toByte())
        list.add((value and 0xFF).toByte())
    }
}
