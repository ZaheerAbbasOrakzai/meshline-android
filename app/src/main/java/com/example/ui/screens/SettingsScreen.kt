package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.data.model.IdentityEntity
import com.example.ui.theme.MeshGreenPrimary
import com.example.ui.theme.MeshMintAccent

@Composable
fun SettingsScreen(
    identity: IdentityEntity?,
    showPanicDialog: Boolean,
    onTogglePanicDialog: () -> Unit,
    onConfirmPanicWipe: () -> Unit,
    onExportIdentity: () -> String?,
    onImportIdentity: (String) -> Unit,
    onUpdateProfile: (String, String) -> Unit
) {
    val myFingerprint = remember(identity?.publicKey) {
        if (identity == null) ""
        else CryptoEngine.calculateSafetyNumberFingerprint(identity.publicKey)
    }
    
    val clipboardManager = LocalClipboardManager.current
    var showImportDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Surface(color = MeshGreenPrimary, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Bluetooth Mesh Profile & Security", fontSize = 12.sp, color = MeshMintAccent)
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
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
                                .clickable { showEditProfileDialog = true }
                        ) {
                            Text(identity?.avatar ?: "ME", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Icon(Icons.Default.Edit, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(16.dp).align(Alignment.BottomEnd))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(identity?.username ?: "Mesh User", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Status: Available on Mesh", fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.Edit, "Edit Profile", tint = MeshMintAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, null, tint = MeshMintAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("P-256 Public Fingerprint:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(myFingerprint, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MeshMintAccent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Identity & Backup", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Button(onClick = { onExportIdentity()?.let { clipboardManager.setText(AnnotatedString(it)) } }, colors = ButtonDefaults.buttonColors(containerColor = MeshGreenPrimary), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Export ID", fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(onClick = { showImportDialog = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Import ID", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onTogglePanicDialog, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Default.DeleteForever, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text("Emergency Wipe Data", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showPanicDialog) {
        AlertDialog(
            onDismissRequest = onTogglePanicDialog,
            title = { Text("Confirm Wipe", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently erase all keys, chats, and contacts.") },
            confirmButton = { Button(onClick = onConfirmPanicWipe, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Wipe Everything") } },
            dismissButton = { TextButton(onClick = onTogglePanicDialog) { Text("Cancel") } }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Identity") },
            text = { OutlinedTextField(value = importText, onValueChange = { importText = it }, placeholder = { Text("Paste ID string...") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { onImportIdentity(importText); showImportDialog = false }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancel") } }
        )
    }

    if (showEditProfileDialog && identity != null) {
        var newName by remember { mutableStateOf(identity.username) }
        var newAvatar by remember { mutableStateOf(identity.avatar) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newAvatar, onValueChange = { newAvatar = it }, label = { Text("Avatar (2 chars)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(onClick = { onUpdateProfile(newName, newAvatar); showEditProfileDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") } }
        )
    }
}
