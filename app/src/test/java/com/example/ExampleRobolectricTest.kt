package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.SecurityManager
import com.example.engine.AudioSynthesizer
import com.example.engine.PhysicsEngine2D
import com.example.engine.graphics.FloatingTextManager
import com.example.engine.graphics.GameBackgroundType
import com.example.engine.graphics.OneButtonGameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Studio AI", appName)
    }

    @Test
    fun `verify physics engine step and spatial hash stability`() {
        val engine = PhysicsEngine2D()
        engine.initSandbox(800f, 1200f)
        assertTrue("Particles should be spawned", engine.particles.isNotEmpty())
        assertTrue("Boxes should be spawned", engine.boxes.isNotEmpty())

        // Run 60 simulation steps to verify zero crash / no NaN / stability
        for (i in 0 until 60) {
            engine.step(0.016f, 800f, 1200f)
        }

        for (p in engine.particles) {
            assertTrue("Particle X must not be NaN", !p.x.isNaN())
            assertTrue("Particle Y must not be NaN", !p.y.isNaN())
            assertTrue("Particle VX must not be NaN", !p.vx.isNaN())
            assertTrue("Particle VY must not be NaN", !p.vy.isNaN())
        }

        // Test burst spawn
        engine.spawnBurst(400f, 600f)
        assertTrue("Active entities should update", engine.activeEntityCount > 0)
    }

    @Test
    fun `verify all 8 game background types metadata`() {
        assertEquals("Must have exactly 8 background types", 8, GameBackgroundType.entries.size)
        val expectedTypes = listOf(
            GameBackgroundType.STATIC_BG,
            GameBackgroundType.TILED_REPEATING_BG,
            GameBackgroundType.SCROLLING_BG,
            GameBackgroundType.GRADIENT_SOLID_BG,
            GameBackgroundType.PROCEDURAL_BG,
            GameBackgroundType.ANIMATED_BG,
            GameBackgroundType.CAMERA_FOLLOW_BG,
            GameBackgroundType.VERTEX_LOW_POLY_3D_BG
        )
        for (type in expectedTypes) {
            assertTrue("Title must not be blank", type.title.isNotBlank())
            assertTrue("Description must not be blank", type.description.isNotBlank())
            assertTrue("Pros must not be blank", type.pros.isNotBlank())
            assertTrue("Cons must not be blank", type.cons.isNotBlank())
            assertTrue("Game examples must not be blank", type.gameExamples.isNotBlank())
        }
    }

    @Test
    fun `verify one button game engine mechanics and stability`() {
        val game = OneButtonGameEngine()
        game.reset(800f, 600f)
        assertTrue("Game should be alive on start", game.isAlive)
        assertEquals("Health must be 3", 3, game.health)
        assertTrue("Items must be populated", game.items.isNotEmpty())

        // Test 1-button jump
        game.controlMode = "JUMP"
        game.onOneButtonAction()
        assertTrue("Vy must be negative after jump", game.playerVy < 0)

        // Run simulation update
        for (i in 0 until 120) {
            game.update(0.016f, 800f, 600f)
        }
        assertTrue("Scroll offset should progress", game.scrollOffset > 0)

        // Test dash mode
        game.controlMode = "DASH"
        game.energy = 100f
        game.onOneButtonAction()
        assertTrue("Should be dashing", game.isDashing)

        // Test gravity flip mode
        game.controlMode = "GRAVITY"
        val origDir = game.gravityDirection
        game.onOneButtonAction()
        assertEquals("Gravity direction must invert", -origDir, game.gravityDirection, 0.001f)
    }

    @Test
    fun `verify zero-alloc floating text manager`() {
        val ftm = FloatingTextManager(poolSize = 16)
        ftm.spawn("COMBO +100", Offset(100f, 200f))
        val activeCount = ftm.pool.count { it.isActive }
        assertEquals("Should have 1 active floating text", 1, activeCount)

        ftm.update(0.1f)
        assertTrue("Y position should drift upward", ftm.pool.first { it.isActive }.y < 200f)
    }

    @Test
    fun `verify security manager AES-256 and checksum integrity`() {
        val sample = "class EngineTest { val code = 42 }"
        val encrypted = SecurityManager.encrypt(sample)
        assertNotNull(encrypted)
        assertTrue("Encrypted output should not be empty", encrypted.isNotBlank())

        val decrypted = SecurityManager.decrypt(encrypted)
        assertEquals("Decrypted text must match original", sample, decrypted)

        val checksum = SecurityManager.computeSha256Checksum(sample)
        assertEquals("Checksum must have 64 hex chars (256-bit)", 64, checksum.length)

        val hmac = SecurityManager.computeHmacSha256(sample)
        assertTrue("HMAC must not be empty", hmac.isNotBlank())
    }

    @Test
    fun `verify audio synthesizer mute state`() {
        AudioSynthesizer.setMuted(true)
        assertTrue(AudioSynthesizer.isMuted())
        AudioSynthesizer.setMuted(false)
        assertTrue(!AudioSynthesizer.isMuted())
    }

    @Test
    fun `verify transfer manager json export and import integrity`() {
        val game = OneButtonGameEngine()
        game.reset(800f, 600f)
        game.controlMode = "GRAVITY"
        game.currentBgType = GameBackgroundType.PROCEDURAL_BG

        val jsonExport = com.example.data.transfer.StudioTransferManager.exportLevelToJson(game, "Cyber Maze Test")
        assertTrue("JSON export must contain packageType", jsonExport.contains("STUDIO_GAME_LEVEL"))
        assertTrue("JSON export must contain backgroundType", jsonExport.contains("PROCEDURAL_BG"))
        assertTrue("JSON export must contain items", jsonExport.contains("items"))

        // Create a new engine and import the json
        val targetGame = OneButtonGameEngine()
        val importResult = com.example.data.transfer.StudioTransferManager.importLevelFromJson(jsonExport, targetGame)
        assertTrue("Import must succeed", importResult.isSuccess)
        assertEquals("Target background must match", GameBackgroundType.PROCEDURAL_BG, targetGame.currentBgType)
        assertEquals("Target control mode must match", "GRAVITY", targetGame.controlMode)
        assertTrue("Target items should be restored", targetGame.items.isNotEmpty())
    }

    @Test
    fun `verify transfer manager encrypted bundle export and import`() {
        val codeSample = "fun computeFps() = 60"
        val pkg = com.example.data.transfer.StudioTransferManager.exportEncryptedProject(
            title = "Fps Engine",
            category = "GAME",
            language = "KOTLIN",
            code = codeSample
        )
        assertTrue("Package must contain STUDIO_PKG_V2", pkg.contains("STUDIO_PKG_V2"))
        assertTrue("Package must contain AES-256", pkg.contains("AES-256-CBC"))

        val importResult = com.example.data.transfer.StudioTransferManager.importEncryptedProject(pkg)
        assertTrue("Import must succeed", importResult.isSuccess)
        val entity = importResult.getOrNull()
        assertNotNull(entity)
        assertEquals("Title must match", "Fps Engine", entity?.title)
        assertEquals("Code must match", codeSample, entity?.codeContent)
    }

    @Test
    fun `verify ai director hardware optimizer profile application`() {
        val game = OneButtonGameEngine()
        val physics = PhysicsEngine2D()
        physics.initSandbox(800f, 1200f)

        val report = com.example.engine.ai.StudioAiDirector.applyHardwareOptimization(
            game,
            physics,
            com.example.engine.ai.StudioAiDirector.PROFILE_ITEL_A70
        )
        assertTrue("Report must mention itel A70", report.contains("itel A70"))
        assertEquals("Background must be STATIC_BG for itel A70", GameBackgroundType.STATIC_BG, game.currentBgType)
        assertTrue("Particle count must be capped to 32", physics.particles.size <= 32)
    }

    @Test
    fun `verify character class archetypes and enemy npc systems`() {
        val game = OneButtonGameEngine()
        game.reset(800f, 600f)

        // Test Character Class Swapping
        game.setPlayerClass(com.example.engine.character.CharacterClass.SOLAR_KNIGHT)
        assertEquals(5, game.health)
        assertEquals(5, game.maxHealth)
        assertTrue(game.shieldActive)

        game.setPlayerClass(com.example.engine.character.CharacterClass.CYBER_NINJA)
        assertEquals(3, game.health)
        assertEquals(2, game.maxJumps)

        // Verify NPCs in spawned world
        val npcs = game.items.filter { it.type == "NPC" }
        assertTrue("World should spawn friendly NPC entities", npcs.isNotEmpty())
        assertNotNull("NPC entity must contain dialogue data", npcs.first().npcData)

        // Verify Enemy types
        val enemies = game.items.filter { it.type == "ENEMY" }
        assertTrue("World should spawn hostile enemies", enemies.isNotEmpty())
        assertTrue("Enemy type must be valid", enemies.any { it.enemyType.damage >= 1 })
    }

    @Test
    fun `verify open source engine ecs tween spatial grid and tilemap systems`() {
        // 1. Test Tween Engine
        val tweenEngine = com.example.engine.core.TweenEngine(maxTweens = 8)
        var tweenVal = 0f
        val tween = tweenEngine.to(
            start = 0f,
            target = 100f,
            duration = 1f,
            ease = com.example.engine.core.EaseType.LINEAR,
            onUpdate = { tweenVal = it }
        )
        assertNotNull(tween)
        tweenEngine.update(0.5f)
        assertEquals(50f, tweenVal, 0.1f)
        tweenEngine.update(0.5f)
        assertEquals(100f, tweenVal, 0.1f)
        assertTrue(tween!!.isComplete)

        // 2. Test Spatial Hash Grid
        val grid = com.example.engine.core.SpatialHashGrid(cellSize = 64f)
        grid.insert(entityId = 1, x = 10f, y = 10f, width = 20f, height = 20f)
        grid.insert(entityId = 2, x = 20f, y = 20f, width = 20f, height = 20f)
        grid.insert(entityId = 3, x = 300f, y = 300f, width = 20f, height = 20f)

        val candidates = IntArray(16)
        val count = grid.queryPotentialColliders(15f, 15f, 20f, 20f, candidates, 16)
        assertTrue("Should detect nearby entities 1 and 2", count >= 2)
        assertTrue(candidates.take(count).contains(1))
        assertTrue(candidates.take(count).contains(2))

        // 3. Test Procedural Tilemap Matrix
        val tilemap = com.example.engine.core.TilemapMatrixEngine(mapWidth = 30, mapHeight = 20)
        tilemap.generateProceduralLevel(com.example.engine.core.ProceduralGeneratorAlgorithm.CELLULAR_AUTOMATA_CAVES)
        var solidCount = 0
        for (y in 0 until 20) {
            for (x in 0 until 30) {
                if (tilemap.getTile(x, y).isSolid) solidCount++
            }
        }
        assertTrue("Cave generator must produce solid rock boundaries", solidCount > 0)

        // 4. Test ECS World
        val ecs = com.example.engine.core.EcsWorld(maxEntities = 16)
        val heroId = ecs.spawnEntity(50f, 50f, 20f, 0f, "HERO")
        assertTrue(heroId >= 0)
        ecs.update(0.016f, 800f, 600f, 1f)
        assertEquals(50f + 20f * 0.016f, ecs.transforms[heroId].x, 0.1f)
    }

    @Test
    fun `verify low level fast binary math bytecode vm bytebeat and rasterizer`() {
        // 1. Fast Binary Math & Quake InvSqrt
        val inv = com.example.engine.core.math.FastBinaryMath.fastInvSqrt(16f)
        assertEquals(0.25f, inv, 0.01f)
        val sqrt = com.example.engine.core.math.FastBinaryMath.fastSqrt(25f)
        assertEquals(5.0f, sqrt, 0.1f)

        val fpA = com.example.engine.core.math.FastBinaryMath.floatToFixed(2.5f)
        val fpB = com.example.engine.core.math.FastBinaryMath.floatToFixed(4.0f)
        val fpMul = com.example.engine.core.math.FastBinaryMath.fixedMul(fpA, fpB)
        assertEquals(10.0f, com.example.engine.core.math.FastBinaryMath.fixedToFloat(fpMul), 0.01f)

        val packed = com.example.engine.core.math.FastBinaryMath.packCoordinates(120.toShort(), 340.toShort())
        assertEquals(120.toShort(), com.example.engine.core.math.FastBinaryMath.unpackX(packed))
        assertEquals(340.toShort(), com.example.engine.core.math.FastBinaryMath.unpackY(packed))

        // 2. Bytecode VM & Text Assembler
        val asmSource = """
            LOAD_CONST R0, 200
            LOAD_CONST R1, 100
            ADD R0, R1
            SET_POS R0, R1
            HALT
        """.trimIndent()
        val bytecode = com.example.engine.core.vm.AssemblerEngine.assemble(asmSource)
        assertTrue(bytecode.isNotEmpty())

        val vm = com.example.engine.core.vm.BytecodeVm()
        vm.loadBytecode(bytecode)
        vm.step(maxCycles = 50)
        assertEquals(300, vm.registers[0])
        assertEquals(100, vm.registers[1])
        assertEquals(300f, vm.entityPosX, 0.01f)
        assertEquals(100f, vm.entityPosY, 0.01f)
        assertTrue(vm.isHalted)

        // 3. Bytebeat DSP Formulas
        val sample1 = com.example.engine.audio.BytebeatDspSynthesizer.evaluateSample(100L, com.example.engine.audio.BytebeatFormula.SIERPINSKI_HARMONY)
        assertTrue(sample1 in 0..255)

        // 4. Procedural Video Rasterizer
        val rasterizer = com.example.engine.graphics.ProceduralVideoRasterizer(width = 32, height = 24)
        rasterizer.clear()
        rasterizer.drawLine(0, 0, 10, 10, 0xFF00FF)
        assertEquals(0xFF00FF, rasterizer.pixelBuffer[0])
        rasterizer.renderRaycast3D(0.5f)
        assertTrue(rasterizer.pixelBuffer.any { it != 0 })
    }

    @Test
    fun `verify binary database serializer and assembly database opcodes`() {
        // 1. Test BytecodeVm DB opcodes (DB_SAVE, DB_LOAD, DB_LOOKUP)
        val dbAsm = """
            LOAD_CONST R0, 100
            LOAD_CONST R1, 500
            LOAD_CONST R2, 0
            DB_SAVE R2
            LOAD_CONST R0, 0
            LOAD_CONST R1, 0
            DB_LOAD R2
            HALT
        """.trimIndent()
        val bc = com.example.engine.core.vm.AssemblerEngine.assemble(dbAsm)
        val vm = com.example.engine.core.vm.BytecodeVm()
        vm.loadBytecode(bc)
        vm.step(maxCycles = 50)
        assertEquals("R0 must be restored to 100", 100, vm.registers[0])
        assertEquals("R1 must be restored to 500", 500, vm.registers[1])

        // 2. Test Binary Database Serializer for VM State
        val serializedVm = com.example.engine.core.vm.BinaryDatabaseSerializer.serializeVmState(vm)
        assertTrue(serializedVm.isNotEmpty())
        val targetVm = com.example.engine.core.vm.BytecodeVm()
        val success = com.example.engine.core.vm.BinaryDatabaseSerializer.deserializeVmState(serializedVm, targetVm)
        assertTrue("Deserialization must succeed", success)
        assertEquals("Target VM R0 must match", 100, targetVm.registers[0])
        assertEquals("Target VM R1 must match", 500, targetVm.registers[1])

        // 3. Test Binary Level Snapshot Serialization
        val snapshot = com.example.engine.core.vm.BinaryDatabaseSerializer.serializeGameSnapshot(
            slotId = 1,
            levelName = "Cyber Run",
            score = 1500,
            health = 3,
            scrollOffset = 240.5f,
            playerY = 120.0f
        )
        assertEquals(1, snapshot.slotId)
        assertEquals("Cyber Run", snapshot.levelName)
        val (scroll, pY) = com.example.engine.core.vm.BinaryDatabaseSerializer.unpackGameSnapshot(snapshot)
        assertEquals(240.5f, scroll, 0.01f)
        assertEquals(120.0f, pY, 0.01f)
    }

    @Test
    fun `verify room database binary program and snapshot entity persistence`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.local.AppDatabase.getDatabase(context)
        val dao = db.studioDao()
        val repo = com.example.data.local.StudioRepository(dao)

        // 1. Test Binary Program Entity
        val dummyBytecode = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x64, 0xFF.toByte())
        val progEntity = com.example.engine.core.vm.BinaryDatabaseSerializer.createBinaryProgramEntity(
            projectId = 101L,
            programName = "FastMathKernel",
            bytecode = dummyBytecode
        )
        val insertedId = repo.saveBinaryProgram(progEntity)
        assertTrue("Inserted program ID must be positive", insertedId > 0)

        val fetched = repo.getBinaryProgram(insertedId)
        assertNotNull("Fetched program must not be null", fetched)
        assertEquals("FastMathKernel", fetched?.programName)
        assertTrue("Bytecode content must match", fetched!!.bytecode.contentEquals(dummyBytecode))

        // 2. Test Game Save Snapshot Entity
        val snapshot = com.example.engine.core.vm.BinaryDatabaseSerializer.serializeGameSnapshot(
            slotId = 2,
            levelName = "Boss Level",
            score = 9999,
            health = 5,
            scrollOffset = 500f,
            playerY = 300f
        )
        repo.saveSnapshot(snapshot)
        val fetchedSnapshot = repo.getSnapshotBySlot(2)
        assertNotNull("Fetched snapshot must not be null", fetchedSnapshot)
        assertEquals("Boss Level", fetchedSnapshot?.levelName)
        assertEquals(9999, fetchedSnapshot?.score)
    }
}
