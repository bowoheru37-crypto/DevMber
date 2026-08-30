package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ai.StudioAiDirector
import com.example.ui.StudioViewModel

@Composable
fun AiProductionDirectorView(viewModel: StudioViewModel) {
    val aiState by viewModel.aiDirectorState.collectAsState()
    val customState by viewModel.deviceCustomization.collectAsState()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xFF00F0FF), Color(0xFF9D4EDD))),
                RoundedCornerShape(16.dp)
            )
            .testTag("ai_production_director_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Copilot",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Production Director & Level Architect",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF00F0FF).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Auto-Pilot", fontSize = 10.sp, color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Ketik ide game/level kamu, AI akan otomatis mengonfigurasi background, physics, items, dan kontrol 1-button secara instan:",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            // Prompt input
            OutlinedTextField(
                value = aiState.userPrompt,
                onValueChange = { viewModel.updateAiDirectorPrompt(it) },
                placeholder = { Text("Contoh: Cyberpunk neon runner dengan gravity flip dan crystal pickups...", fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("ai_level_prompt_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00F0FF),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedContainerColor = Color(0xFF0A0E17),
                    unfocusedContainerColor = Color(0xFF0A0E17),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(10.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
            )

            // AI Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Space Runner" to "Space runner with procedural starfield and double jumps",
                        "Gravity Maze" to "Gravity flip maze with slime bouncers and purple crystals",
                        "Monument 3D" to "Monument Valley low poly 3D world with coin collector"
                    ).forEach { (label, presetPrompt) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.updateAiDirectorPrompt(presetPrompt)
                                viewModel.generateAiLevelBlueprint()
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF0A0E17),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                Button(
                    onClick = { viewModel.generateAiLevelBlueprint() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !aiState.isGenerating,
                    modifier = Modifier.testTag("generate_ai_level_btn")
                ) {
                    if (aiState.isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF00363D), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Color(0xFF00363D), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Generate", fontSize = 11.sp, color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Status output
            if (aiState.optimizationStatus.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0E17))
                        .padding(8.dp)
                ) {
                    Text(
                        text = aiState.optimizationStatus,
                        fontSize = 10.sp,
                        color = Color(0xFF06D6A0)
                    )
                }
            }

            // Low-Entry Hardware Optimizer (itel A70 & Android 5+)
            Text(
                text = "⚡ Hardware Optimization Profile (itel A70 / Unisoc T603 / Android 5+):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB703),
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isItel = customState.selectedProfile == StudioAiDirector.PROFILE_ITEL_A70
                FilterChip(
                    selected = isItel,
                    onClick = { viewModel.applyHardwareProfile(StudioAiDirector.PROFILE_ITEL_A70) },
                    label = { Text("itel A70 (Ultra-Cool)", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00F0FF),
                        selectedLabelColor = Color(0xFF00363D),
                        containerColor = Color(0xFF0A0E17),
                        labelColor = Color(0xFF94A3B8)
                    )
                )

                val isEco = customState.selectedProfile == StudioAiDirector.PROFILE_BATTERY_SAVER_ECO
                FilterChip(
                    selected = isEco,
                    onClick = { viewModel.applyHardwareProfile(StudioAiDirector.PROFILE_BATTERY_SAVER_ECO) },
                    label = { Text("Eco 30FPS", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFB703),
                        selectedLabelColor = Color(0xFF00363D),
                        containerColor = Color(0xFF0A0E17),
                        labelColor = Color(0xFF94A3B8)
                    )
                )

                val isBalanced = customState.selectedProfile == StudioAiDirector.PROFILE_BALANCED_MOBILE
                FilterChip(
                    selected = isBalanced,
                    onClick = { viewModel.applyHardwareProfile(StudioAiDirector.PROFILE_BALANCED_MOBILE) },
                    label = { Text("Balanced 60FPS", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF06D6A0),
                        selectedLabelColor = Color(0xFF00363D),
                        containerColor = Color(0xFF0A0E17),
                        labelColor = Color(0xFF94A3B8)
                    )
                )
            }
        }
    }
}
