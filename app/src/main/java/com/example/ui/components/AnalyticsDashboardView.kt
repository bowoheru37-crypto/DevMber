package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SecurityManager
import com.example.ui.StudioViewModel

@Composable
fun AnalyticsDashboardView(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val userSession by viewModel.userSession.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val analyticsState by viewModel.analyticsState.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()

    var isEncryptionEnabled by rememberSaveable { mutableStateOf(true) }

    // Live Crypto Testing Sandbox State
    var rawCryptoInput by rememberSaveable { mutableStateOf("Engine Vector Matrix (x=10, y=25)") }
    var encryptedOutput by rememberSaveable { mutableStateOf("") }
    var decryptedOutput by rememberSaveable { mutableStateOf("") }
    var shaOutput by rememberSaveable { mutableStateOf("") }

    // Live JVM Memory
    val runtime = Runtime.getRuntime()
    val usedMemoryMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).coerceAtLeast(1)
    val maxMemoryMb = (runtime.maxMemory() / (1024 * 1024))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User & Firebase Authentication Card
        item(key = "user_auth_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00F0FF),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = userSession?.displayName ?: "Studio Developer",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = userSession?.email ?: "Offline mode session active",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        if (userSession == null || userSession?.isAnonymous == true) {
                            Button(
                                onClick = { viewModel.signInGoogle(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("google_sign_in_button")
                            ) {
                                Text("Sign In", color = Color(0xFF00363D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.signOut() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Text("Sign Out", color = Color(0xFFFF4D6D), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Firestore Real-Time Cloud Sync & Offline State
        item(key = "cloud_sync_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (syncState.isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (syncState.isSyncing) Color(0xFFFFB703) else Color(0xFF06D6A0),
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Cloud Sync (Firestore)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.triggerSyncNow() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("trigger_sync_button")
                        ) {
                            if (syncState.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF00F0FF), strokeWidth = 2.dp)
                            } else {
                                Text("Sync Now", fontSize = 11.sp, color = Color(0xFF00F0FF))
                            }
                        }
                    }

                    Text(
                        text = syncState.statusMessage,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // System Diagnostics & Performance Telemetry (Low-Entry Device Guard)
        item(key = "hardware_diagnostics_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(20.dp))
                        Text(
                            text = "Hardware Diagnostics (itel A70 / Unisoc T603 Profile)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00F0FF)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Heap Allocated", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("${usedMemoryMb} MB / ${maxMemoryMb} MB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06D6A0))
                        }

                        Column {
                            Text("GC Pressure", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("Zero-Alloc Loop", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                        }

                        Column {
                            Text("Target Architecture", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("Android 5.0 - 15", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB703))
                        }
                    }
                }
            }
        }

        // Security & End-to-End AES-256 Engine Card
        item(key = "security_encryption_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color(0xFF06D6A0), modifier = Modifier.size(20.dp))
                            Column {
                                Text("AES-256 + HMAC-SHA256 Security", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Real-time payload encryption & integrity tester", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        Switch(
                            checked = isEncryptionEnabled,
                            onCheckedChange = { isEncryptionEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF06D6A0),
                                checkedTrackColor = Color(0xFF00382B)
                            )
                        )
                    }

                    OutlinedTextField(
                        value = rawCryptoInput,
                        onValueChange = { rawCryptoInput = it },
                        placeholder = { Text("Enter string or payload to encrypt...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0A0E17),
                            unfocusedContainerColor = Color(0xFF0A0E17),
                            focusedBorderColor = Color(0xFF06D6A0),
                            unfocusedBorderColor = Color(0xFF2A374A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                encryptedOutput = SecurityManager.encrypt(rawCryptoInput)
                                shaOutput = SecurityManager.computeSha256Checksum(rawCryptoInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06D6A0)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00363D), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Encrypt", fontSize = 11.sp, color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (encryptedOutput.isNotBlank()) {
                                    decryptedOutput = SecurityManager.decrypt(encryptedOutput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Decrypt", fontSize = 11.sp, color = Color(0xFF00F0FF))
                        }
                    }

                    if (encryptedOutput.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0E17), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Encrypted Base64: $encryptedOutput", fontSize = 10.sp, color = Color(0xFF06D6A0), fontFamily = FontFamily.Monospace)
                                Text("SHA-256 Hash: $shaOutput", fontSize = 10.sp, color = Color(0xFF00F0FF), fontFamily = FontFamily.Monospace)
                                if (decryptedOutput.isNotBlank()) {
                                    Text("Decrypted Plaintext: $decryptedOutput", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Push Notifications Simulator
        item(key = "push_notifications_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(20.dp))
                            Text("Personalized Push Notifications", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.triggerLocalNotification("AI Engine has optimized rendering pipeline for high battery efficiency.") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Send Alert", fontSize = 10.sp, color = Color(0xFFFFB703))
                        }
                    }

                    Text(
                        text = "Last Alert: ${analyticsState.lastNotificationMessage}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Saved Project Library in Room
        item(key = "saved_projects_header") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Project Storage (${allProjects.size} saved in Room Database)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (allProjects.isEmpty()) {
                        Text("No saved projects yet.", fontSize = 11.sp, color = Color(0xFF64748B))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            allProjects.forEach { project ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0A0E17), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(project.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("${project.language} • ${project.category}", fontSize = 10.sp, color = Color(0xFF00F0FF))
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { viewModel.loadProject(project) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Load", fontSize = 10.sp, color = Color.White)
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteProject(project.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF4D6D), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
