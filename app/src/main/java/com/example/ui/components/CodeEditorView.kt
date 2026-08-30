package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.web.NodeJsRuntimeEngine
import com.example.engine.web.ReactRuntimeEngine
import com.example.engine.web.TypeScriptEngine
import com.example.engine.web.WebBuilderEngine
import com.example.ui.StudioViewModel

@Composable
fun CodeEditorView(viewModel: StudioViewModel) {
    val editorState by viewModel.codeEditorState.collectAsState()
    var aiPromptInput by rememberSaveable { mutableStateOf("") }
    var activeSubTab by rememberSaveable { mutableIntStateOf(0) } // 0: Editor, 1: React Live Mount, 2: Node.js Terminal, 3: TS Diagnostics
    var terminalInput by rememberSaveable { mutableStateOf("npm run build") }
    var terminalLogs by rememberSaveable { mutableStateOf(listOf("📦 Studio Node.js Virtual Terminal Ready.", "Type 'npm install <pkg>', 'node server.js', or 'curl /api/health'")) }
    var reactCounterState by rememberSaveable { mutableIntStateOf(0) }
    var activeViewportName by rememberSaveable { mutableStateOf(WebBuilderEngine.ViewportMode.MOBILE_COMPACT.name) }

    val languages = listOf("GOLANG", "RUST", "WASM", "JAVA", "PYTHON", "REACT_TSX", "TYPESCRIPT", "NODEJS", "JAVASCRIPT", "KOTLIN", "GLSL", "HTML5")

    val presets = listOf(
        "GOLANG_CONCURRENCY" to "Go 1.23 Channels",
        "RUST_BORROW_SIMD" to "Rust 2024 SIMD",
        "WASM_WAT_STACK_VM" to "WASM WAT Stack",
        "JAVA_VIRTUAL_THREADS" to "Java 21 Loom",
        "PYTHON_AUTOGRAD_AI" to "Python Autograd",
        "REACT_TSX_CYBER" to "React 19 TSX",
        "NODEJS_EXPRESS_API" to "Node Express",
        "REACT_THREE_FIBER" to "React 3D Three",
        "TYPESCRIPT_WASM_DSP" to "TS Wasm DSP",
        "KINEMATICS_SOLVER" to "Kinematics 2D",
        "WEB_CANVAS_ARCADE" to "HTML5 Arcade",
        "WEB_AUDIO_VISUALIZER" to "Web Audio Synth",
        "GLSL_RAYMARCHER" to "GLSL Shader",
        "PARTICLE_JS" to "JS Particles",
        "FAST_MATH_ASM" to "Fast Math ASM"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Item 1: Header & Control Bar
        item(key = "editor_header_bar") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = editorState.currentTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Target: ${editorState.currentLanguage} • Zero Heap Allocation",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { viewModel.openExportModal("CODE_PROJECT") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Export Bundle", tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { viewModel.openImportModal("CODE_PROJECT") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Import Bundle", tint = Color(0xFF06D6A0), modifier = Modifier.size(18.dp))
                            }

                            Button(
                                onClick = { viewModel.saveCurrentProject() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("save_project_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Save Project",
                                    tint = Color(0xFF00F0FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", fontSize = 12.sp, color = Color.White)
                            }

                            Button(
                                onClick = { viewModel.executeCode() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06D6A0)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("execute_code_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Execute",
                                    tint = Color(0xFF00363D),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run", fontSize = 12.sp, color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Algorithm Presets Chips with LazyRow
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Presets",
                                    tint = Color(0xFFFFB703),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Presets:", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        items(presets, key = { it.first }) { (key, label) ->
                            SuggestionChip(
                                onClick = { viewModel.loadPreset(key) },
                                label = { Text(label, fontSize = 10.sp, color = Color(0xFFE2E8F0)) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF0A0E17)),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = Color(0xFF2A374A)
                                )
                            )
                        }
                    }

                    // Language Filter Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(languages, key = { it }) { lang ->
                            val isSelected = editorState.currentLanguage == lang
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateLanguage(lang) },
                                label = { Text(lang, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00F0FF),
                                    selectedLabelColor = Color(0xFF00363D),
                                    containerColor = Color(0xFF0A0E17),
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFF2A374A),
                                    selectedBorderColor = Color(0xFF00F0FF)
                                )
                            )
                        }
                    }

                    // Multi-Tab IDE Bar (Source Code, React Live, Node Terminal, TS Types, Polyglot Matrix)
                    TabRow(
                        selectedTabIndex = activeSubTab,
                        containerColor = Color(0xFF0A0E17),
                        contentColor = Color(0xFF00F0FF),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                                color = Color(0xFF00F0FF)
                            )
                        },
                        modifier = Modifier.background(Color(0xFF0A0E17), RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = activeSubTab == 0,
                            onClick = { activeSubTab = 0 },
                            text = { Text("Code", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = activeSubTab == 1,
                            onClick = { activeSubTab = 1 },
                            text = { Text("React 19", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = activeSubTab == 2,
                            onClick = { activeSubTab = 2 },
                            text = { Text("Node/NPM", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = activeSubTab == 3,
                            onClick = { activeSubTab = 3 },
                            text = { Text("TS Types", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = activeSubTab == 4,
                            onClick = { activeSubTab = 4 },
                            text = { Text("Polyglot", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }

        // Sub-Tab 0: Source Code Editor
        if (activeSubTab == 0) {
            item(key = "code_editor_box") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E17)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Source Editor (${editorState.currentLanguage}) • ${editorState.codeText.lines().size} lines",
                                fontSize = 11.sp,
                                color = Color(0xFF00F0FF),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "UTF-8 | LF",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Mobile IDE Quick-Toolbar
                        val quickSymbols = listOf("TAB", "{ }", "( )", "[ ]", "=>", "=", "+", "-", "*", "/", "<", ">", "\"", ":", ";", ".")
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(quickSymbols, key = { it }) { symbol ->
                                SuggestionChip(
                                    onClick = {
                                        val insertion = when (symbol) {
                                            "TAB" -> "    "
                                            "{ }" -> "{\n    \n}"
                                            "( )" -> "()"
                                            "[ ]" -> "[]"
                                            "=>" -> " => "
                                            else -> symbol
                                        }
                                        viewModel.updateCodeText(editorState.codeText + insertion)
                                    },
                                    label = { Text(symbol, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF)) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF131B2A)),
                                    border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = Color(0xFF1E293B))
                                )
                            }
                        }

                        OutlinedTextField(
                            value = editorState.codeText,
                            onValueChange = { viewModel.updateCodeText(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .testTag("code_editor_field"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 18.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                cursorColor = Color(0xFF00F0FF)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Output Console Card
            item(key = "output_terminal_card") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Engine Console & Sandbox Output",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF06D6A0)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0E17), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = editorState.executionLog,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }

        // Sub-Tab 1: React 19 Live Virtual DOM Mount & Hook Preview
        if (activeSubTab == 1) {
            item(key = "react_live_mount_card") {
                val vdom = remember(editorState.codeText) {
                    ReactRuntimeEngine.parseJsxSnippet(editorState.codeText)
                }
                val diff = remember(vdom, reactCounterState) {
                    ReactRuntimeEngine.reconcile(null, vdom)
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E17)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Web, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(20.dp))
                                Text("⚛️ React 19 Interactive Live Mount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text("Fiber Nodes: ${diff.fiberNodesCount}", fontSize = 11.sp, color = Color(0xFF00F0FF), fontFamily = FontFamily.Monospace)
                        }

                        // Simulated Mounted Component Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF131B2A), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Component: <${vdom.type} />", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                                    Text("State: $reactCounterState", fontSize = 12.sp, color = Color(0xFF06D6A0), fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = vdom.text ?: "Interactive React 19 Hook & JSX Container mounted on virtual fiber tree.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE2E8F0)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { reactCounterState++ },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("useState(+1)", fontSize = 11.sp, color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { reactCounterState = 0 },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reset State", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Virtual DOM Diagnostics
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⚡ Reconciliation Profile:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            Text("• Reconciliation Time: ${diff.reconciliationTimeMicros} μs (Zero-GC)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF06D6A0))
                            Text("• React 19 Compiler: Auto-Memoization Active (No useMemo overhead)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFCBD5E1))
                            Text("• PWA Standalone Bundle: Ready for export & cloud sync", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFFFB703))
                        }
                    }
                }
            }
        }

        // Sub-Tab 2: Node.js 22 LTS Terminal & NPM Package Manager
        if (activeSubTab == 2) {
            item(key = "nodejs_terminal_card") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E17)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF06D6A0), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF06D6A0), modifier = Modifier.size(20.dp))
                                Text("🟩 Node.js v22.4.0 Virtual Terminal & NPM", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text("LTS Active", fontSize = 10.sp, color = Color(0xFF06D6A0), fontWeight = FontWeight.Bold)
                        }

                        // Terminal Log Screen
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color(0xFF070A10), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(terminalLogs) { log ->
                                    Text(
                                        text = log,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (log.startsWith("✔") || log.startsWith("✓")) Color(0xFF06D6A0) else if (log.startsWith("📦") || log.startsWith("🚀")) Color(0xFF00F0FF) else Color(0xFF94A3B8),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        // Command Line Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = terminalInput,
                                onValueChange = { terminalInput = it },
                                placeholder = { Text("npm install three, node server.js, curl /api/health...") },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedBorderColor = Color(0xFF06D6A0),
                                    unfocusedBorderColor = Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Button(
                                onClick = {
                                    if (terminalInput.isNotBlank()) {
                                        val res = NodeJsRuntimeEngine.runTerminalCommand(terminalInput)
                                        terminalLogs = terminalLogs + "❯ $terminalInput" + res.logs
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06D6A0)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Exec", fontSize = 12.sp, color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Installed NPM Packages Registry
                        Text("Installed NPM Packages in Virtual Workspace:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(NodeJsRuntimeEngine.npmRegistry, key = { it.name }) { pkg ->
                                SuggestionChip(
                                    onClick = {
                                        val res = NodeJsRuntimeEngine.runTerminalCommand("npm install ${pkg.name}")
                                        terminalLogs = terminalLogs + "❯ npm install ${pkg.name}" + res.logs
                                    },
                                    label = {
                                        Text(
                                            text = "${pkg.name}@${pkg.version} (${pkg.sizeKb}KB)",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (pkg.isInstalled) Color(0xFF06D6A0) else Color(0xFF94A3B8)
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF131B2A)),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        enabled = true,
                                        borderColor = if (pkg.isInstalled) Color(0xFF06D6A0) else Color(0xFF1E293B)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sub-Tab 3: TypeScript 5.5 Diagnostics & AST Type Inspector
        if (activeSubTab == 3) {
            item(key = "typescript_diagnostics_card") {
                val tsResult = remember(editorState.codeText) {
                    TypeScriptEngine.analyzeAndTranspile(editorState.codeText)
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E17)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF3178C6), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Extension, contentDescription = null, tint = Color(0xFF3178C6), modifier = Modifier.size(20.dp))
                                Text("🟦 TypeScript 5.5 Static Type Checker", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text("Strict: ON", fontSize = 10.sp, color = Color(0xFF3178C6), fontWeight = FontWeight.Bold)
                        }

                        // Type Check Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF131B2A), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("Inferred Types", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text("${tsResult.inferredTypesCount} Signatures", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF131B2A), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("Transpile Time", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text("${tsResult.transpilationTimeMicros} μs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06D6A0))
                                }
                            }
                        }

                        // Generated .d.ts Ambient Definitions Preview
                        Text("Generated Ambient Declarations (.d.ts):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = tsResult.declarationsDts,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Sub-Tab 4: Polyglot Multi-Engine Execution Matrix (Go, Rust, Wasm, Java, Python, Node, TS, React)
        if (activeSubTab == 4) {
            item(key = "polyglot_engine_matrix_card") {
                val polyReport = remember(editorState.currentLanguage, editorState.codeText) {
                    com.example.engine.polyglot.PolyglotEngineHub.executePolyglotCode(
                        editorState.currentLanguage,
                        editorState.codeText
                    )
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E17)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFFB703), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(20.dp))
                                Text(
                                    text = "⚡ Polyglot Multi-Engine Matrix",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "${polyReport.language.iconLabel} Active",
                                fontSize = 11.sp,
                                color = Color(0xFFFFB703),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Engine Performance Telemetry Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF131B2A), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("Execution Speed", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text("${polyReport.executionTimeMicros} μs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06D6A0))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF131B2A), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("Memory Footprint", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text("${polyReport.memoryFootprintKb} KB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF131B2A), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("Lines Analyzed", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text("${polyReport.linesCount} Lines", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB703))
                                }
                            }
                        }

                        // Runtime Status & Diagnostics Logs
                        Text("Engine Diagnostics & Execution Logs:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = polyReport.statusMessage,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (polyReport.isSuccess) Color(0xFF06D6A0) else Color(0xFFFF2A85)
                                )
                                polyReport.detailedLogs.forEach { log ->
                                    Text(
                                        text = log,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF94A3B8),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        // Quick Polyglot Engine Switcher Chips
                        Text("Switch Simulation Engine:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                listOf(
                                    "GOLANG" to "🐹 Go 1.23",
                                    "RUST" to "🦀 Rust 2024",
                                    "WASM" to "🌐 Wasm WAT",
                                    "JAVA" to "☕ Java 21 LTS",
                                    "PYTHON" to "🐍 Python 3.12",
                                    "REACT_TSX" to "⚛️ React 19",
                                    "TYPESCRIPT" to "🟦 TS 5.5",
                                    "NODEJS" to "🟩 Node.js"
                                )
                            ) { (langKey, label) ->
                                SuggestionChip(
                                    onClick = {
                                        viewModel.updateLanguage(langKey)
                                    },
                                    label = { Text(label, fontSize = 10.sp, color = Color(0xFFE2E8F0)) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF131B2A)),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        enabled = true,
                                        borderColor = if (editorState.currentLanguage == langKey) Color(0xFFFFB703) else Color(0xFF2A374A)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Item: Gemini AI High-Thinking Engine Card
        item(key = "gemini_ai_thinking_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF9D4EDD), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color(0xFF9D4EDD),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "AI High-Thinking Architect",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "gemini-3.1-pro-preview with ThinkingLevel.HIGH",
                                fontSize = 11.sp,
                                color = Color(0xFF9D4EDD)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = aiPromptInput,
                        onValueChange = { aiPromptInput = it },
                        placeholder = { Text("Describe React component, TypeScript interface, Node.js API, or physics formula...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_high_thinking_prompt_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0A0E17),
                            unfocusedContainerColor = Color(0xFF0A0E17),
                            focusedBorderColor = Color(0xFF9D4EDD),
                            unfocusedBorderColor = Color(0xFF2A374A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (aiPromptInput.isNotBlank()) {
                                    viewModel.requestAiHighThinking(aiPromptInput)
                                }
                            },
                            enabled = !editorState.isAiThinking,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("submit_ai_thinking_button")
                        ) {
                            if (editorState.isAiThinking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Thinking...", fontSize = 12.sp, color = Color.White)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Solve with Deep Reasoning", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Thought Reasoning Display
                    AnimatedVisibility(
                        visible = editorState.aiReasoningLog.isNotBlank(),
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "🧠 Internal Thinking & Chain-of-Thought Trace:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00F0FF)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = editorState.aiReasoningLog,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Global Transfer & Import/Export Modal
    TransferModalDialog(viewModel = viewModel)
}
