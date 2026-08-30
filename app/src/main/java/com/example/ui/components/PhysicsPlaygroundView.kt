package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AudioSynthesizer
import com.example.engine.character.CharacterClass
import com.example.engine.character.CharacterSystemRenderer
import com.example.engine.character.EnemyType
import com.example.engine.character.NpcEntity
import com.example.engine.physics.BodyType
import com.example.engine.physics.PhysicsBody
import com.example.engine.graphics.BackgroundRenderer
import com.example.engine.graphics.GameBackgroundType
import com.example.engine.graphics.OneButtonGameEngine
import com.example.engine.graphics.SpriteRenderer
import com.example.ui.StudioViewModel
import kotlin.math.sin

enum class EngineStudioMode {
    ONE_BUTTON_GAME_BG,
    OPEN_SOURCE_ENGINE,
    SPRITE_ATLAS_STUDIO,
    PHYSICS_KINEMATICS
}

@Composable
fun PhysicsPlaygroundView(viewModel: StudioViewModel) {
    var currentModeName by rememberSaveable { mutableStateOf(EngineStudioMode.ONE_BUTTON_GAME_BG.name) }
    val currentMode = try {
        EngineStudioMode.valueOf(currentModeName)
    } catch (e: Exception) {
        EngineStudioMode.ONE_BUTTON_GAME_BG
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Studio Mode Navigation Tabs
        TabRow(
            selectedTabIndex = currentMode.ordinal,
            containerColor = Color(0xFF131B2A),
            contentColor = Color(0xFF00F0FF),
            indicator = { tabPositions ->
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[currentMode.ordinal])
                        .height(3.dp)
                        .background(Color(0xFF00F0FF), RoundedCornerShape(2.dp))
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = currentMode == EngineStudioMode.ONE_BUTTON_GAME_BG,
                onClick = { currentModeName = EngineStudioMode.ONE_BUTTON_GAME_BG.name },
                text = { Text("1-Button & 8-BG", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Gamepad, contentDescription = "Game & BG", modifier = Modifier.size(16.dp)) },
                selectedContentColor = Color(0xFF00F0FF),
                unselectedContentColor = Color(0xFF94A3B8)
            )
            Tab(
                selected = currentMode == EngineStudioMode.OPEN_SOURCE_ENGINE,
                onClick = { currentModeName = EngineStudioMode.OPEN_SOURCE_ENGINE.name },
                text = { Text("ECS & Tilemap", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.DeveloperBoard, contentDescription = "Open Engine", modifier = Modifier.size(16.dp)) },
                selectedContentColor = Color(0xFF00F0FF),
                unselectedContentColor = Color(0xFF94A3B8)
            )
            Tab(
                selected = currentMode == EngineStudioMode.SPRITE_ATLAS_STUDIO,
                onClick = { currentModeName = EngineStudioMode.SPRITE_ATLAS_STUDIO.name },
                text = { Text("Sprite & Atlas", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Layers, contentDescription = "Sprite Atlas", modifier = Modifier.size(16.dp)) },
                selectedContentColor = Color(0xFF00F0FF),
                unselectedContentColor = Color(0xFF94A3B8)
            )
            Tab(
                selected = currentMode == EngineStudioMode.PHYSICS_KINEMATICS,
                onClick = { currentModeName = EngineStudioMode.PHYSICS_KINEMATICS.name },
                text = { Text("2D Physics", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Speed, contentDescription = "Physics", modifier = Modifier.size(16.dp)) },
                selectedContentColor = Color(0xFF00F0FF),
                unselectedContentColor = Color(0xFF94A3B8)
            )
        }

        when (currentMode) {
            EngineStudioMode.ONE_BUTTON_GAME_BG -> OneButtonGameWithBackgroundsView(viewModel)
            EngineStudioMode.OPEN_SOURCE_ENGINE -> OpenEngineStudioView(viewModel)
            EngineStudioMode.SPRITE_ATLAS_STUDIO -> SpriteAtlasStudioView()
            EngineStudioMode.PHYSICS_KINEMATICS -> PhysicsKinematicsSandboxView(viewModel)
        }

        // Global Transfer & Import/Export Modal
        TransferModalDialog(viewModel = viewModel)
    }
}

/**
 * 1. UNIFIED 1-BUTTON GAME & 8 BACKGROUND TYPES SHOWCASE
 */
@Composable
fun OneButtonGameWithBackgroundsView(viewModel: StudioViewModel) {
    val gameEngine = remember { viewModel.oneButtonEngine }
    var selectedBgName by rememberSaveable { mutableStateOf(GameBackgroundType.STATIC_BG.name) }
    val selectedBg = try {
        GameBackgroundType.valueOf(selectedBgName)
    } catch (e: Exception) {
        GameBackgroundType.STATIC_BG
    }

    var isMuted by rememberSaveable { mutableStateOf(false) }
    var fpsDisplay by remember { mutableIntStateOf(60) }
    var frameCounter by remember { mutableIntStateOf(0) }
    var lastFpsTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var canvasSize by remember { mutableStateOf(Size(800f, 600f)) }

    // Game loop tick
    LaunchedEffect(Unit) {
        var lastTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastTimeNanos != 0L) {
                    val dt = (frameTimeNanos - lastTimeNanos) / 1_000_000_000f
                    gameEngine.currentBgType = selectedBg
                    gameEngine.update(dt, canvasSize.width, canvasSize.height)
                }
                lastTimeNanos = frameTimeNanos
                frameCounter++

                val now = System.currentTimeMillis()
                if (now - lastFpsTime >= 1000) {
                    fpsDisplay = frameCounter
                    frameCounter = 0
                    lastFpsTime = now
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Background Type Selector Carousel (All 8 types)
        item(key = "bg_type_carousel") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(GameBackgroundType.entries, key = { it.name }) { bg ->
                    val isSelected = selectedBg == bg
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedBgName = bg.name
                            gameEngine.currentBgType = bg
                            AudioSynthesizer.playToneAsync(620f, durationMs = 50, volume = 0.15f)
                        },
                        label = {
                            Text(
                                text = bg.shortName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) Color(0xFF00363D) else Color(0xFF94A3B8)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00F0FF),
                            selectedLabelColor = Color(0xFF00363D),
                            containerColor = Color(0xFF131B2A),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }

        // Live Interactive Game Viewport
        item(key = "game_canvas_viewport") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF060913))
                    .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .testTag("one_button_game_canvas")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                gameEngine.onOneButtonAction()
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    canvasSize = size
                    val w = size.width
                    val h = size.height

                    // 1. Render Background based on selected type
                    BackgroundRenderer.render(
                        drawScope = this,
                        bgType = selectedBg,
                        width = w,
                        height = h,
                        scrollOffset = gameEngine.scrollOffset,
                        animTime = gameEngine.globalAnimTime,
                        cameraX = gameEngine.cameraX,
                        cameraY = gameEngine.cameraY
                    )

                    // 2. Render Platforms / Floor Line
                    val floorY = h * 0.82f
                    drawLine(
                        color = Color(0xFF00F0FF),
                        start = Offset(0f, floorY),
                        end = Offset(w, floorY),
                        strokeWidth = 2.5f
                    )

                    // 3. Render Items, Enemies, NPCs & Hazards
                    for (item in gameEngine.items) {
                        if (item.isCollected) continue
                        val screenItemX = item.x - gameEngine.scrollOffset + gameEngine.playerX

                        if (screenItemX in -40f..(w + 40f)) {
                            when (item.type) {
                                "COIN" -> SpriteRenderer.drawGoldCoin(
                                    drawScope = this,
                                    x = screenItemX,
                                    y = item.y,
                                    size = item.size,
                                    animTime = gameEngine.globalAnimTime
                                )
                                "CRYSTAL" -> SpriteRenderer.drawPowerCrystal(
                                    drawScope = this,
                                    x = screenItemX,
                                    y = item.y,
                                    size = item.size,
                                    animTime = gameEngine.globalAnimTime
                                )
                                "ENEMY" -> CharacterSystemRenderer.drawEnemy(
                                    drawScope = this,
                                    x = screenItemX,
                                    y = item.y,
                                    size = item.size,
                                    enemyType = item.enemyType,
                                    animTime = gameEngine.globalAnimTime
                                )
                                "NPC" -> {
                                    val npc = item.npcData ?: NpcEntity("npc_0", "Guide", "Ally", Color(0xFF00F0FF), "Safe journey!")
                                    CharacterSystemRenderer.drawNpc(
                                        drawScope = this,
                                        x = screenItemX,
                                        y = item.y,
                                        npc = npc,
                                        animTime = gameEngine.globalAnimTime
                                    )
                                }
                                "SPIKE" -> SpriteRenderer.drawHazardSpike(
                                    drawScope = this,
                                    x = screenItemX,
                                    y = item.y,
                                    width = item.size * 1.3f,
                                    height = item.size
                                )
                            }
                        }
                    }

                    // 4. Render Hero Player Character with Archetype and Shield
                    CharacterSystemRenderer.drawPlayableCharacter(
                        drawScope = this,
                        x = gameEngine.playerX,
                        y = gameEngine.playerY,
                        radius = gameEngine.playerRadius,
                        characterClass = gameEngine.characterClass,
                        isJumping = !gameEngine.isGrounded,
                        isDashing = gameEngine.isDashing,
                        animTime = gameEngine.globalAnimTime,
                        shieldActive = gameEngine.shieldActive
                    )

                    // 5. Render Zero-Allocation Floating Combat Text Popups
                    for (ft in gameEngine.floatingTexts.pool) {
                        if (ft.isActive) {
                            drawCircle(
                                color = ft.color.copy(alpha = ft.alpha * 0.9f),
                                radius = 12f * ft.scale,
                                center = Offset(ft.x, ft.y)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = ft.alpha),
                                radius = 6f * ft.scale,
                                center = Offset(ft.x, ft.y)
                            )
                        }
                    }
                }

                // HUD Overlay (Score, Combo, Health, Energy)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top HUD Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Score & High Score
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "SCORE: ${gameEngine.score}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFFFB703),
                                    fontFamily = FontFamily.Monospace
                                )
                                if (gameEngine.combo > 1) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFF2A85), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${gameEngine.combo}x COMBO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${gameEngine.characterClass.title} | HI: ${gameEngine.highScore}",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // Dynamic Health Hearts based on Character baseHp
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(gameEngine.maxHealth) { index ->
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Heart",
                                    tint = if (index < gameEngine.health) Color(0xFFFF2A85) else Color(0xFF334155),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Bottom HUD: Energy Gauge & Tap Hint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Energy bar for Dash
                        Column(modifier = Modifier.width(110.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ENERGY", fontSize = 9.sp, color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                                Text("${gameEngine.energy.toInt()}%", fontSize = 9.sp, color = Color.White)
                            }
                            LinearProgressIndicator(
                                progress = { gameEngine.energy / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF00F0FF),
                                trackColor = Color(0xFF1E293B)
                            )
                        }

                        // 1-Button Tap Hint Bubble
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF131B2A).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = Color(0xFF00F0FF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "TAP SCREEN TO ${gameEngine.controlMode}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00F0FF)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gameplay Mode Controls & Settings Bar
        item(key = "gameplay_mode_controls") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("JUMP", "DASH", "GRAVITY").forEach { mode ->
                        val isSelected = gameEngine.controlMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                gameEngine.controlMode = mode
                                AudioSynthesizer.playToneAsync(500f, durationMs = 50, volume = 0.15f)
                            },
                            label = { Text(mode, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00F0FF),
                                selectedLabelColor = Color(0xFF00363D),
                                containerColor = Color(0xFF131B2A),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            AudioSynthesizer.setMuted(isMuted)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Audio Toggle",
                            tint = if (isMuted) Color(0xFF94A3B8) else Color(0xFF9D4EDD),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = {
                            gameEngine.reset(800f, 600f)
                            AudioSynthesizer.playToneAsync(440f, durationMs = 80, volume = 0.2f)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("reset_game_engine")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart", tint = Color(0xFF00F0FF), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Character Archetype Selection Row (Cyber Ninja, Solar Knight, Void Warlock, Mecha Titan)
        item(key = "character_archetype_selection") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(CharacterClass.entries, key = { it.name }) { archetype ->
                    val isSelected = gameEngine.characterClass == archetype
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            gameEngine.setPlayerClass(archetype)
                        },
                        label = { Text(archetype.title, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = archetype.primaryColor,
                            selectedLabelColor = Color(0xFF00363D),
                            containerColor = Color(0xFF131B2A),
                            labelColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }

        // Import / Export and AI Level Quick Toolbar
        item(key = "import_export_toolbar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.openExportModal("GAME_LEVEL") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131B2A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("export_level_btn")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Export", tint = Color(0xFF00F0FF), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.openImportModal("GAME_LEVEL") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131B2A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("import_level_btn")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Import", tint = Color(0xFF06D6A0), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import Level", fontSize = 11.sp, color = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(14.dp))
                        Text("itel A70: 0 GC Pause", fontSize = 10.sp, color = Color(0xFFFFB703), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // AI Production Director & Level Architect View
        item(key = "ai_production_director_section") {
            AiProductionDirectorView(viewModel = viewModel)
        }

        // Comprehensive Background Detail Card (List Jenis Background Game)
        item(key = "background_detail_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedBg.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF00F0FF)
                        )
                        Text(
                            text = "$fpsDisplay FPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF06D6A0),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("• Apa:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFFFB703), modifier = Modifier.width(55.dp))
                        Text(selectedBg.description, fontSize = 11.sp, color = Color(0xFFE2E8F0))
                    }

                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("• Plus:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF06D6A0), modifier = Modifier.width(55.dp))
                        Text(selectedBg.pros, fontSize = 11.sp, color = Color(0xFFE2E8F0))
                    }

                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("• Minus:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFFF4D6D), modifier = Modifier.width(55.dp))
                        Text(selectedBg.cons, fontSize = 11.sp, color = Color(0xFFE2E8F0))
                    }

                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("• Dipake di:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF9D4EDD), modifier = Modifier.width(55.dp))
                        Text(selectedBg.gameExamples, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * 2. SPRITE, TILE & ATLAS STUDIO
 */
@Composable
fun SpriteAtlasStudioView() {
    var selectedSprite by rememberSaveable { mutableStateOf("HERO_PLAYER") }
    var animTimer by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastTime != 0L) {
                    animTimer += (frameNanos - lastTime) / 1_000_000_000f
                }
                lastTime = frameNanos
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sprite Catalog Selector
        item(key = "sprite_catalog_selector") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    listOf(
                        "HERO_PLAYER" to "Hero (4 Classes)",
                        "ENEMY_ROSTER" to "Enemies (4 Types)",
                        "NPC_ROSTER" to "NPC Dialogue",
                        "GOLD_COIN" to "Gold Coin (3D)",
                        "POWER_CRYSTAL" to "Power Gem",
                        "HAZARD_SPIKE" to "Hazard Spike"
                    ),
                    key = { it.first }
                ) { (id, label) ->
                    val isSelected = selectedSprite == id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSprite = id },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00F0FF),
                            selectedLabelColor = Color(0xFF00363D),
                            containerColor = Color(0xFF131B2A),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }

        // Texture Atlas Visual Slicer Card
        item(key = "texture_atlas_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Texture Atlas Matrix & Character Engine (512x512)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)

                    // Interactive Atlas Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0A0E17))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Grid lines
                            val stepX = w / 4f
                            val stepY = h / 2f
                            for (i in 0..4) {
                                drawLine(Color(0xFF1E293B), Offset(i * stepX, 0f), Offset(i * stepX, h), 1f)
                            }
                            for (j in 0..2) {
                                drawLine(Color(0xFF1E293B), Offset(0f, j * stepY), Offset(w, j * stepY), 1f)
                            }

                            // Row 1: Playable Character Archetypes
                            CharacterSystemRenderer.drawPlayableCharacter(this, stepX * 0.5f, stepY * 0.5f, radius = 20f, characterClass = CharacterClass.CYBER_NINJA, animTime = animTimer)
                            CharacterSystemRenderer.drawPlayableCharacter(this, stepX * 1.5f, stepY * 0.5f, radius = 20f, characterClass = CharacterClass.SOLAR_KNIGHT, animTime = animTimer)
                            CharacterSystemRenderer.drawPlayableCharacter(this, stepX * 2.5f, stepY * 0.5f, radius = 20f, characterClass = CharacterClass.VOID_WARLOCK, animTime = animTimer)
                            CharacterSystemRenderer.drawPlayableCharacter(this, stepX * 3.5f, stepY * 0.5f, radius = 20f, characterClass = CharacterClass.MECHA_TITAN, animTime = animTimer)

                            // Row 2: Enemy Archetypes & NPC Quest Giver
                            CharacterSystemRenderer.drawEnemy(this, stepX * 0.5f, stepY * 1.5f, size = 22f, enemyType = EnemyType.SLIME_BOUNCER, animTime = animTimer)
                            CharacterSystemRenderer.drawEnemy(this, stepX * 1.5f, stepY * 1.5f, size = 22f, enemyType = EnemyType.CYBER_DRONE, animTime = animTimer)
                            CharacterSystemRenderer.drawEnemy(this, stepX * 2.5f, stepY * 1.5f, size = 22f, enemyType = EnemyType.VOID_STALKER, animTime = animTimer)
                            CharacterSystemRenderer.drawNpc(this, stepX * 3.5f, stepY * 1.5f, NpcEntity("npc_demo", "Oracle", "Sage", Color(0xFF00F0FF), "Explore the matrix!"), animTime = animTimer)
                        }
                    }

                    // Sprite Inspector Details
                    val details = when (selectedSprite) {
                        "HERO_PLAYER" -> "Playable Archetypes: Cyber Ninja (Dash invulnerability), Solar Knight (Radiant Shield), Void Warlock (Antigravity), Mecha Titan (Heavy armor smash)."
                        "ENEMY_ROSTER" -> "Enemy AI Archetypes: Slime Bouncers (hop patrol), Cyber Drones (sine hover dive), Void Stalkers (3-eye ambush), Mecha Titan Golems (heavy ground stomp)."
                        "NPC_ROSTER" -> "NPC Interactive System: Zero-alloc friendly quest givers with floating quest indicator and dynamic score reward dialogue."
                        "GOLD_COIN" -> "Gold Coin: Dynamic 3D rotation projection matrix using trigonometric horizontal scaling."
                        "POWER_CRYSTAL" -> "Power Gem: Faceted diamond shader with pulsing purple aura and float sine motion."
                        else -> "Hazard Spike: High-contrast triangular vector obstacle with white danger rim."
                    }
                    Text(details, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

/**
 * 3. 2D RIGID BODY DYNAMICS & KINEMATICS SANDBOX
 */
@Composable
fun PhysicsKinematicsSandboxView(viewModel: StudioViewModel) {
    val engine = viewModel.physicsEngine
    val sandbox = viewModel.gameSandbox

    var gravityScale by rememberSaveable { mutableFloatStateOf(980f) }
    var currentTouchMode by rememberSaveable { mutableStateOf("DRAG_RIGID") }
    var selectedPreset by rememberSaveable { mutableStateOf("KINEMATIC_PLATFORMS") }
    var spawnBodyType by rememberSaveable { mutableStateOf(BodyType.DYNAMIC) }
    var userRestitution by rememberSaveable { mutableFloatStateOf(0.65f) }
    var userFriction by rememberSaveable { mutableFloatStateOf(0.40f) }
    var userDensity by rememberSaveable { mutableFloatStateOf(1.0f) }
    var showBodyConfig by rememberSaveable { mutableStateOf(false) }

    var fpsDisplay by remember { mutableIntStateOf(60) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsCalcTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Sync parameters into engine
    LaunchedEffect(spawnBodyType, userRestitution, userFriction, userDensity) {
        engine.spawnBodyType = spawnBodyType
        engine.spawnRestitution = userRestitution
        engine.spawnFriction = userFriction
        engine.spawnDensity = userDensity
    }

    LaunchedEffect(Unit) {
        var lastTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastTimeNanos != 0L) {
                    val dt = (frameTimeNanos - lastTimeNanos) / 1_000_000_000f
                    engine.gravity = gravityScale
                    engine.touchMode = currentTouchMode
                    engine.step(dt, 1080f, 1600f)
                }
                lastTimeNanos = frameTimeNanos
                frameCount++

                val now = System.currentTimeMillis()
                if (now - lastFpsCalcTime >= 1000) {
                    fpsDisplay = frameCount
                    frameCount = 0
                    lastFpsCalcTime = now
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Preset Scenarios Bar
        item(key = "kinematics_preset_bar") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Rigid Dynamics Benchmark Scenarios:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB703))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "KINEMATIC_PLATFORMS" to "Kinematic Movers & Mix",
                        "BOX_PYRAMID" to "Box Pyramid Stack",
                        "NEWTONS_CRADLE" to "Newton's Cradle",
                        "ROPE_BRIDGE" to "Rope Bridge Chains",
                        "DESTRUCTION_CANNON" to "Demolition Cannon",
                        "PINBALL_PLAYGROUND" to "Pinball Bouncers"
                    )
                    items(presets, key = { it.first }) { (id, label) ->
                        val isSelected = selectedPreset == id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPreset = id
                                engine.setPreset(id, 1000f, 1400f)
                                sandbox.playSynthTone("659 Hz (E5)", freq = 659.25f)
                            },
                            label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00F0FF),
                                selectedLabelColor = Color(0xFF00363D),
                                containerColor = Color(0xFF131B2A),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }
        }

        // Mode & Reset Controls
        item(key = "kinematics_controls_bar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DRAG_RIGID" to "Drag Bodies", "SPAWN" to "Spawn", "REPEL" to "Repel", "ATTRACT" to "Attract").forEach { (mode, label) ->
                        val isSelected = currentTouchMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { currentTouchMode = mode },
                            label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF2A85),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF131B2A),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showBodyConfig = !showBodyConfig },
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (showBodyConfig) Color(0xFF00F0FF).copy(alpha = 0.2f) else Color(0xFF1E293B))
                    ) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = "Config", tint = Color(0xFF00F0FF), modifier = Modifier.size(16.dp))
                    }

                    Button(
                        onClick = { engine.setPreset(selectedPreset, 1000f, 1400f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("reset_physics_sandbox")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFF00F0FF), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // PhysicsBody Material & Property Inspector Panel
        if (showBodyConfig || currentTouchMode == "SPAWN") {
            item(key = "physics_body_property_inspector") {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡ PhysicsBody Spawn & Material Tuning:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                            Text("Mass: ${(userDensity * 10).toInt()}kg | e: ${String.format("%.2f", userRestitution)} | μ: ${String.format("%.2f", userFriction)}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }

                        // Body Type Selector (Static vs Dynamic vs Kinematic)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Body Type:", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            listOf(
                                BodyType.DYNAMIC to "Dynamic (Forces)",
                                BodyType.KINEMATIC to "Kinematic (Mover)",
                                BodyType.STATIC to "Static (Immovable)"
                            ).forEach { (type, label) ->
                                val isSelected = spawnBodyType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { spawnBodyType = type },
                                    label = { Text(label, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when(type) {
                                            BodyType.DYNAMIC -> Color(0xFF00F0FF)
                                            BodyType.KINEMATIC -> Color(0xFF9D4EDD)
                                            BodyType.STATIC -> Color(0xFF64748B)
                                        },
                                        selectedLabelColor = if (type == BodyType.DYNAMIC) Color(0xFF00363D) else Color.White,
                                        containerColor = Color(0xFF0A0E17),
                                        labelColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }

                        // Restitution (Elasticity) Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Restitution (e): ${String.format("%.2f", userRestitution)}", fontSize = 10.sp, color = Color(0xFF06D6A0), modifier = Modifier.width(130.dp))
                            Slider(
                                value = userRestitution,
                                onValueChange = { userRestitution = it },
                                valueRange = 0.0f..1.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF06D6A0), activeTrackColor = Color(0xFF06D6A0))
                            )
                        }

                        // Friction Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Friction (μ): ${String.format("%.2f", userFriction)}", fontSize = 10.sp, color = Color(0xFFFFB703), modifier = Modifier.width(130.dp))
                            Slider(
                                value = userFriction,
                                onValueChange = { userFriction = it },
                                valueRange = 0.0f..1.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFB703), activeTrackColor = Color(0xFFFFB703))
                            )
                        }

                        // Density / Mass Multiplier Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Density / Mass: ${String.format("%.1f", userDensity)}x", fontSize = 10.sp, color = Color(0xFFFF2A85), modifier = Modifier.width(130.dp))
                            Slider(
                                value = userDensity,
                                onValueChange = { userDensity = it },
                                valueRange = 0.2f..4.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFF2A85), activeTrackColor = Color(0xFFFF2A85))
                            )
                        }
                    }
                }
            }
        }

        // Gravity Slider
        item(key = "kinematics_gravity_slider") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Gravity: ${(gravityScale / 100).toInt()} m/s²",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.width(90.dp)
                )
                Slider(
                    value = gravityScale,
                    onValueChange = { gravityScale = it },
                    valueRange = 0f..2000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00F0FF),
                        activeTrackColor = Color(0xFF00F0FF),
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Interactive Canvas Viewport
        item(key = "kinematics_canvas_viewport") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0A0E17))
                    .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .testTag("physics_canvas_box")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                engine.onTouchDown(offset.x, offset.y)
                                if (currentTouchMode == "SPAWN") {
                                    engine.spawnBurst(offset.x, offset.y)
                                    sandbox.playSynthTone("523 Hz (C5 Pop)", freq = 523.25f)
                                } else {
                                    sandbox.playSynthTone(
                                        if (currentTouchMode == "ATTRACT") "440 Hz (A4)" else "330 Hz (E4)",
                                        freq = if (currentTouchMode == "ATTRACT") 440f else 329.6f
                                    )
                                }
                                tryAwaitRelease()
                                engine.onTouchUp()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                engine.onTouchDown(offset.x, offset.y)
                            },
                            onDragEnd = {
                                engine.onTouchUp()
                            },
                            onDragCancel = {
                                engine.onTouchUp()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                engine.onTouchMove(change.position.x, change.position.y)
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasW = size.width
                    val canvasH = size.height

                    // Cybernetic background grid
                    val gridSize = 40.dp.toPx()
                    var x = 0f
                    while (x < canvasW) {
                        drawLine(Color(0xFF131B2A), Offset(x, 0f), Offset(x, canvasH), 1f)
                        x += gridSize
                    }
                    var y = 0f
                    while (y < canvasH) {
                        drawLine(Color(0xFF131B2A), Offset(0f, y), Offset(canvasW, y), 1f)
                        y += gridSize
                    }

                    // Render Full Rigid Body Dynamics World
                    engine.world.render(this)

                    // Render dynamic particles
                    for (p in engine.particles) {
                        val px = p.x.coerceIn(p.radius, canvasW - p.radius)
                        val py = p.y.coerceIn(p.radius, canvasH - p.radius)
                        drawCircle(color = p.color, radius = p.radius, center = Offset(px, py))
                    }

                    // Draw touch force ripple indicator
                    if (engine.touchForceActive && currentTouchMode != "DRAG_RIGID") {
                        drawCircle(
                            color = if (currentTouchMode == "ATTRACT") Color(0xFF00F0FF).copy(alpha = 0.35f) else Color(0xFFFF2A85).copy(alpha = 0.35f),
                            radius = 80f,
                            center = engine.touchPos,
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        }

        // Live Real-Time Telemetry Card
        item(key = "kinematics_telemetry_card") {
            val dynamicCount = engine.world.bodies.count { it is PhysicsBody.Dynamic }
            val kinematicCount = engine.world.bodies.count { it is PhysicsBody.Kinematic }
            val staticCount = engine.world.bodies.count { it is PhysicsBody.Static }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SIM SOLVER", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            Text("${engine.lastStepMicros} μs / step", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06D6A0))
                        }
                        Column {
                            Text("TOTAL BODIES", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            Text("${engine.world.bodies.size} bodies", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                        }
                        Column {
                            Text("CONTACTS (SAT)", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            Text("${engine.world.contactManifoldCount} pairs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB703))
                        }
                        Column {
                            Text("FRAME RATE", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            Text("$fpsDisplay FPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF2A85))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Color(0xFF00F0FF).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text("Dynamic: $dynamicCount", fontSize = 9.sp, color = Color(0xFF00F0FF), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Surface(color = Color(0xFF9D4EDD).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text("Kinematic: $kinematicCount", fontSize = 9.sp, color = Color(0xFF9D4EDD), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Surface(color = Color(0xFF64748B).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text("Static: $staticCount", fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
