package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.data.model.IdentityEntity
import com.example.ui.theme.MeshGreenPrimary
import com.example.ui.theme.MeshMintAccent

import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

@Composable
fun SettingsScreen(
    identity: IdentityEntity?,
    showPanicDialog: Boolean,
    onTogglePanicDialog: () -> Unit,
    onConfirmPanicWipe: () -> Unit,
    onExportIdentity: () -> String?,
    onImportIdentity: (String) -> Unit
) {
    val myFingerprint = remember(identity?.publicKey) {
        if (identity == null) ""
        else CryptoEngine.calculateSafetyNumberFingerprint(identity.publicKey)
    }
    
    val clipboardManager = LocalClipboardManager.current
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Surface(
            color = MeshGreenPrimary,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Bluetooth Mesh Profile & Security",
                    fontSize = 12.sp,
                    color = MeshMintAccent
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Identity Profile Card (WhatsApp / Telegram style)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(MeshGreenPrimary)
                        ) {
                            Text(
                                text = identity?.avatar ?: "ME",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = identity?.username ?: "Mesh User",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MeshMintAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "OPEN NODE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MeshMintAccent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Status: Available on Bluetooth Mesh",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MeshMintAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Your Public ECDH P-256 Fingerprint:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = myFingerprint,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MeshMintAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Backup & Restore Card
            Text(
                text = "Backup & Mesh Identity",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = {
                                onExportIdentity()?.let {
                                    clipboardManager.setText(AnnotatedString(it))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MeshGreenPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export ID", fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Import ID", fontSize = 12.sp)
                        }
                    }
                    Text(
                        "Backup your P-256 keys to move your mesh identity to another device.",
                        fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy & Open-Source Architecture Overview
            Text(
                text = "Open-Source & Privacy Architecture",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingFeatureItem(
                        icon = Icons.Default.Bluetooth,
                        title = "100% Bluetooth P2P Network",
                        description = "Direct peer-to-peer and multi-hop mesh relays without cellular data, WiFi routers, or central cloud servers."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingFeatureItem(
                        icon = Icons.Default.Lock,
                        title = "Zero Server Footprint",
                        description = "All cryptographic keys and chats reside exclusively in your local on-device encrypted Room database."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingFeatureItem(
                        icon = Icons.Default.Shield,
                        title = "Decentralized & Open to All",
                        description = "No admin approval needed. Anyone can discover nearby peers, join the broadcast mesh, and communicate freely."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingFeatureItem(
                        icon = Icons.Default.Key,
                        title = "AES-256-GCM Authenticated Encryption",
                        description = "Every message and voice note is encrypted end-to-end with per-session ephemeral keys and 12-byte IVs."
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Emergency Panic Button Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1517)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Emergency Panic Data Destruction",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B6B)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Instantly destroy all local cryptographic keys, chat logs, and verified contacts permanently.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onTogglePanicDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("panic_wipe_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Wipe All Local Storage",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Panic Confirmation Modal
    if (showPanicDialog) {
        AlertDialog(
            onDismissRequest = onTogglePanicDialog,
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFFF4D4D)
                )
            },
            title = {
                Text(
                    text = "Confirm Emergency Data Wipe",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This action will permanently erase your ECDH keys, private passphrase salt, all message logs, and contact pairs. This operation cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmPanicWipe,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D))
                ) {
                    Text("Permanently Wipe Data", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onTogglePanicDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Identity String") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    placeholder = { Text("Paste 1|username|pub|priv|salt string...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onImportIdentity(importText)
                    showImportDialog = false
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MeshMintAccent.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MeshMintAccent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
