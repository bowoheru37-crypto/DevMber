package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.engine.ai.StudioAiDirector
import com.example.ui.StudioViewModel
import kotlin.math.roundToInt

@Composable
fun FloatingQuickTool(
    viewModel: StudioViewModel,
    isOpen: Boolean,
    onToggle: () -> Unit
) {
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var quickPrompt by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        if (!isOpen) {
            // Minimized Floating Pill / Bubble
            Surface(
                onClick = onToggle,
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF131B2A),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .testTag("floating_ai_pill")
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(Color(0xFF00F0FF), Color(0xFF9D4EDD))
                        ),
                        RoundedCornerShape(28.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00F0FF))
                    )
                    Text(
                        text = "AI Copilot",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Copilot icon",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            // Expanded Dialog / Overlay Box
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(Color(0xFF00F0FF), Color(0xFF9D4EDD), Color(0xFFFF2A85))
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .testTag("floating_ai_dialog")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "High-Thinking Copilot",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onToggle,
                            modifier = Modifier.size(28.dp).testTag("close_floating_copilot")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "Powered by gemini-3.1-pro-preview (ThinkingLevel.HIGH)",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = quickPrompt,
                        onValueChange = { quickPrompt = it },
                        placeholder = { Text("Ask for architecture, math, code fix...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .testTag("floating_prompt_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color(0xFF2A374A),
                            focusedContainerColor = Color(0xFF0A0E17),
                            unfocusedContainerColor = Color(0xFF0A0E17),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )

                    // Quick 1-Tap Workflows
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.applyHardwareProfile(StudioAiDirector.PROFILE_ITEL_A70)
                                onToggle()
                            },
                            label = { Text("itel A70 Boost", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF0A0E17), labelColor = Color(0xFF00F0FF))
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.openExportModal("GAME_LEVEL")
                                onToggle()
                            },
                            label = { Text("Export Level", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF0A0E17), labelColor = Color(0xFF06D6A0))
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.generateAiLevelBlueprint()
                                onToggle()
                            },
                            label = { Text("AI Level Blueprint", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF0A0E17), labelColor = Color(0xFFFFB703))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (quickPrompt.isNotBlank()) {
                                    viewModel.requestAiHighThinking(quickPrompt)
                                    quickPrompt = ""
                                    onToggle()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00F0FF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("send_floating_prompt")
                        ) {
                            Text(
                                "Analyze & Solve",
                                color = Color(0xFF00363D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Color(0xFF00363D),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
