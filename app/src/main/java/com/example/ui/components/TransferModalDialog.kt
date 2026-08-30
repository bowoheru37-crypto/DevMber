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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.StudioViewModel

@Composable
fun TransferModalDialog(viewModel: StudioViewModel) {
    val modalState by viewModel.transferModalState.collectAsState()
    val context = LocalContext.current

    if (!modalState.isOpen) return

    Dialog(onDismissRequest = { viewModel.closeTransferModal() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(20.dp))
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(Color(0xFF00F0FF), Color(0xFF9D4EDD))),
                    RoundedCornerShape(20.dp)
                )
                .testTag("transfer_modal_card")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
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
                            imageVector = if (modalState.mode == "EXPORT") Icons.Default.CloudDownload else Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (modalState.mode == "EXPORT") "Export & Download Package" else "Import & Upload Package",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = { viewModel.closeTransferModal() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Type Selector (Game Level vs Code Project)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = modalState.targetType == "GAME_LEVEL",
                        onClick = {
                            if (modalState.mode == "EXPORT") viewModel.openExportModal("GAME_LEVEL")
                            else viewModel.openImportModal("GAME_LEVEL")
                        },
                        label = { Text("1-Button Level (JSON)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00F0FF),
                            selectedLabelColor = Color(0xFF00363D),
                            containerColor = Color(0xFF0A0E17),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )

                    FilterChip(
                        selected = modalState.targetType == "CODE_PROJECT",
                        onClick = {
                            if (modalState.mode == "EXPORT") viewModel.openExportModal("CODE_PROJECT")
                            else viewModel.openImportModal("CODE_PROJECT")
                        },
                        label = { Text("Encrypted Bundle (AES-256)", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF9D4EDD),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF0A0E17),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }

                // Content Payload Text Box
                OutlinedTextField(
                    value = modalState.payloadText,
                    onValueChange = { viewModel.updateTransferPayloadText(it) },
                    placeholder = {
                        Text(
                            text = if (modalState.mode == "EXPORT") "Export payload will appear here..." else "Paste JSON level or .studiopkg text here...",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("transfer_payload_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00F0FF),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFF0A0E17),
                        unfocusedContainerColor = Color(0xFF0A0E17),
                        focusedTextColor = Color(0xFF00F0FF),
                        unfocusedTextColor = Color(0xFFE2E8F0)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Status message indicator
                if (modalState.statusMessage.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0A0E17), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = modalState.statusMessage,
                            fontSize = 10.sp,
                            color = if (modalState.statusMessage.startsWith("❌")) Color(0xFFFF4D6D) else Color(0xFF06D6A0)
                        )
                    }
                }

                // Action Buttons (Copy, Paste, Execute)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (modalState.mode == "EXPORT") {
                            Button(
                                onClick = { viewModel.copyTransferPayloadToClipboard(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("copy_payload_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF00F0FF), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy All", fontSize = 11.sp, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.pasteFromClipboardToTransfer(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("paste_payload_btn")
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color(0xFF00F0FF), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (modalState.mode == "EXPORT") {
                                viewModel.copyTransferPayloadToClipboard(context)
                                viewModel.closeTransferModal()
                            } else {
                                viewModel.performImportAction(context)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (modalState.mode == "EXPORT") Color(0xFF00F0FF) else Color(0xFF06D6A0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("submit_transfer_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = Color(0xFF00363D),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (modalState.mode == "EXPORT") "Done & Copy" else "Apply Import",
                            color = Color(0xFF00363D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
