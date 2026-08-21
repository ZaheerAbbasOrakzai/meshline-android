package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.data.model.IdentityEntity
import com.example.mesh.MeshPeer
import com.example.ui.theme.MeshGreenPrimary
import com.example.ui.theme.MeshMintAccent
import com.example.ui.theme.MeshSignalMedium
import com.example.ui.theme.MeshSignalStrong
import com.example.ui.theme.MeshSignalWeak

@Composable
fun PairNewNodeDialog(
    identity: IdentityEntity?,
    discoveredNodes: List<MeshPeer>,
    onDismiss: () -> Unit,
    onSendJoinRequest: (name: String, nodeId: String, pubKey: String) -> Unit,
    onTriggerSimulatedBeacon: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var manualNodeId by remember { mutableStateOf("") }
    var manualNodeName by remember { mutableStateOf("") }

    val myFingerprint = remember(identity?.publicKey) {
        if (identity == null) ""
        else CryptoEngine.calculateSafetyNumberFingerprint(identity.publicKey)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MeshMintAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Join & Pair Mesh Nodes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MeshMintAccent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Discovered (${discoveredNodes.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Manual Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("My Node ID & QR", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> {
                        // Discovered Nearby BLE Nodes
                        if (discoveredNodes.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BluetoothSearching,
                                        contentDescription = null,
                                        tint = MeshMintAccent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Scanning for unjoined nodes...",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Ensure nearby device has Bluetooth enabled and Meshline active.",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = onTriggerSimulatedBeacon,
                                        colors = ButtonDefaults.buttonColors(containerColor = MeshMintAccent),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.testTag("simulate_beacon_in_pair_dialog")
                                    ) {
                                        Text(
                                            text = "+ Detect Test Peer Node",
                                            color = Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                            ) {
                                items(discoveredNodes, key = { it.nodeId }) { node ->
                                    DiscoveredNodeItem(
                                        node = node,
                                        onJoin = {
                                            val dummyKp = CryptoEngine.generateEcdhKeyPair()
                                            onSendJoinRequest(
                                                node.name,
                                                node.nodeId,
                                                CryptoEngine.encodePublicKey(dummyKp.public)
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    1 -> {
                        // Manual Connect
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Enter another user's Mesh Node ID or Public Key to initiate ECDH connection request over BLE hops:",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = manualNodeName,
                                onValueChange = { manualNodeName = it },
                                label = { Text("Peer Display Name (e.g. Nomad-Unit-2)", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = manualNodeId,
                                onValueChange = { manualNodeId = it },
                                label = { Text("Node ID (e.g. mesh_node_4481)", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    if (manualNodeName.isNotBlank() && manualNodeId.isNotBlank()) {
                                        val dummyKp = CryptoEngine.generateEcdhKeyPair()
                                        onSendJoinRequest(
                                            manualNodeName,
                                            manualNodeId,
                                            CryptoEngine.encodePublicKey(dummyKp.public)
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MeshMintAccent),
                                shape = RoundedCornerShape(16.dp),
                                enabled = manualNodeName.isNotBlank() && manualNodeId.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_manual_join_request_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Send Mesh Join Request",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    2 -> {
                        // My Node ID & QR Representation
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Share your offline Mesh Node Identity with nearby users for instant zero-trust pairing:",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Vector QR Grid Canvas
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(8.dp)
                            ) {
                                Canvas(modifier = Modifier.size(130.dp)) {
                                    val count = 9
                                    val cellSize = size.width / count
                                    val hash = (identity?.publicKey ?: "mesh_node_zaheer").hashCode()

                                    for (i in 0 until count) {
                                        for (j in 0 until count) {
                                            val isCorner = (i < 3 && j < 3) || (i > 5 && j < 3) || (i < 3 && j > 5)
                                            val bit = ((hash xor (i * 31 + j * 17)) and (1 shl ((i + j) % 16))) != 0
                                            if (isCorner || bit) {
                                                drawRect(
                                                    color = Color.Black,
                                                    topLeft = Offset(i * cellSize, j * cellSize),
                                                    size = Size(cellSize * 0.9f, cellSize * 0.9f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Node ID: ${identity?.username ?: "Zaheer Abbas"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Fingerprint: $myFingerprint",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MeshMintAccent
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Meshline Node ID", identity?.publicKey ?: "")
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Node ID Copied to Clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Node ID",
                                            tint = MeshMintAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MeshMintAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun DiscoveredNodeItem(
    node: MeshPeer,
    onJoin: () -> Unit
) {
    val signalColor = when {
        node.rssi > -60 -> MeshSignalStrong
        node.rssi > -80 -> MeshSignalMedium
        else -> MeshSignalWeak
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MeshGreenPrimary)
                ) {
                    Text(
                        text = node.name.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = node.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${node.nodeId} · ${node.rssi} dBm",
                        fontSize = 10.sp,
                        color = signalColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(containerColor = MeshMintAccent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "Request Join",
                    fontSize = 10.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
