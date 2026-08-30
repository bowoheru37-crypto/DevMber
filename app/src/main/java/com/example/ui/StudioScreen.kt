package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnalyticsDashboardView
import com.example.ui.components.CodeEditorView
import com.example.ui.components.FloatingQuickTool
import com.example.ui.components.MediaStudioView
import com.example.ui.components.PhysicsPlaygroundView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(viewModel: StudioViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val floatingOpen by viewModel.floatingWidgetOpen.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17)),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Studio AI Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openExportModal("GAME_LEVEL") },
                        modifier = Modifier.testTag("top_bar_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Export Package",
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.openImportModal("GAME_LEVEL") },
                        modifier = Modifier.testTag("top_bar_import_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Import Package",
                            tint = Color(0xFF06D6A0),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleFloatingWidget() },
                        modifier = Modifier.testTag("top_bar_toggle_copilot")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Copilot",
                            tint = Color(0xFFFFB703),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0E17),
                    titleContentColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF131B2A),
                contentColor = Color.White,
                modifier = Modifier
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == StudioTab.CODE_STUDIO,
                    onClick = { viewModel.selectTab(StudioTab.CODE_STUDIO) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Code Studio") },
                    label = { Text("Code", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00363D),
                        selectedTextColor = Color(0xFF00F0FF),
                        indicatorColor = Color(0xFF00F0FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == StudioTab.PHYSICS_SANDBOX,
                    onClick = { viewModel.selectTab(StudioTab.PHYSICS_SANDBOX) },
                    icon = { Icon(Icons.Default.Games, contentDescription = "Physics Sandbox") },
                    label = { Text("Sandbox", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00363D),
                        selectedTextColor = Color(0xFF00F0FF),
                        indicatorColor = Color(0xFF00F0FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == StudioTab.AI_CREATIVE,
                    onClick = { viewModel.selectTab(StudioTab.AI_CREATIVE) },
                    icon = { Icon(Icons.Default.Palette, contentDescription = "AI Creative Media") },
                    label = { Text("Creative", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00363D),
                        selectedTextColor = Color(0xFF00F0FF),
                        indicatorColor = Color(0xFF00F0FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == StudioTab.ANALYTICS_CLOUD,
                    onClick = { viewModel.selectTab(StudioTab.ANALYTICS_CLOUD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Analytics & Cloud") },
                    label = { Text("Cloud & Ops", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00363D),
                        selectedTextColor = Color(0xFF00F0FF),
                        indicatorColor = Color(0xFF00F0FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0A0E17))
        ) {
            when (currentTab) {
                StudioTab.CODE_STUDIO -> CodeEditorView(viewModel)
                StudioTab.PHYSICS_SANDBOX -> PhysicsPlaygroundView(viewModel)
                StudioTab.AI_CREATIVE -> MediaStudioView(viewModel)
                StudioTab.ANALYTICS_CLOUD -> AnalyticsDashboardView(viewModel)
            }

            // Draggable & Minimized Floating AI Quick Tool
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                FloatingQuickTool(
                    viewModel = viewModel,
                    isOpen = floatingOpen,
                    onToggle = { viewModel.toggleFloatingWidget() }
                )
            }
        }
    }
}
