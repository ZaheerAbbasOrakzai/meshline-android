package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JoinRequestEntity
import com.example.mesh.MeshPeer
import com.example.mesh.MeshRoute
import com.example.mesh.PacketLogEntry
import com.example.ui.theme.*

@Composable
fun NetworkDashboardScreen(
    peers: List<MeshPeer>,
    routes: List<MeshRoute>,
    packetLogs: List<PacketLogEntry>,
    pendingJoinRequests: List<JoinRequestEntity>,
    isScanning: Boolean,
    isRelayMode: Boolean,
    onStartScan: () -> Unit,
    onToggleRelay: () -> Unit,
    onOpenJoinRequests: () -> Unit,
    onOpenPairDialog: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "Radar")
    val pulseScale by transition.animateFloat(
        initialValue = 0.95f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "Pulse"
    )

    Scaffold { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Mesh Topology", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Decentralized P2P Network", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MeshMintAccent)
                    }
                    Button(onClick = onStartScan, colors = ButtonDefaults.buttonColors(containerColor = MeshMintAccent), shape = RoundedCornerShape(20.dp)) {
                        Icon(imageVector = Icons.Default.BluetoothSearching, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isScanning) "Scanning..." else "Scan Mesh", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            if (pendingJoinRequests.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MeshMintAccent.copy(0.15f)), modifier = Modifier.fillMaxWidth().clickable { onOpenJoinRequests() }) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).clip(CircleShape).background(MeshMintAccent)) { 
                                    Icon(imageVector = Icons.Default.Sensors, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp)) 
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("${pendingJoinRequests.size} Pending Join Requests", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Nearby device wants to connect", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Button(onClick = onOpenJoinRequests, colors = ButtonDefaults.buttonColors(containerColor = MeshMintAccent), shape = RoundedCornerShape(16.dp), modifier = Modifier.height(32.dp)) {
                                Text("Review", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MeshGreenPrimary), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp).scale(if (isScanning) pulseScale else 1f).clip(CircleShape).background(Color.White.copy(0.15f)).border(2.dp, MeshMintAccent, CircleShape)) {
                            Icon(imageVector = Icons.Default.Hub, contentDescription = null, tint = MeshMintAccent, modifier = Modifier.size(42.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Mesh Active: ${peers.size} Verified Links", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatMetric("Max TTL", "7 Hops")
                            StatMetric("Avg Latency", "< 20ms")
                            StatMetric("Encryption", "AES-256")
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CellTower, contentDescription = null, tint = MeshMintAccent, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Relay Mode", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("Forward packets for multi-hop routing", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Switch(checked = isRelayMode, onCheckedChange = { onToggleRelay() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = MeshMintAccent))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Connected Mesh Peers (${peers.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    IconButton(onClick = onOpenPairDialog) { Icon(imageVector = Icons.Default.Add, contentDescription = "Add Peer", tint = MeshMintAccent) }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (peers.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                        Text("No Direct Links. Scan to discover nodes.", Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            } else {
                items(peers, key = { it.id }) { peer ->
                    PeerNodeCard(peer)
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                Spacer(Modifier.height(14.dp))
                Text("Live Packet Traffic", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
            }

            if (packetLogs.isEmpty()) {
                item { Text("Waiting for traffic...", fontSize = 12.sp, color = Color.Gray) }
            } else {
                items(packetLogs.take(12)) { log ->
                    PacketLogCard(log)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun StatMetric(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 11.sp, color = Color.LightGray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun PeerNodeCard(peer: MeshPeer) {
    val signalColor = if (peer.rssi > -60) MeshSignalStrong else if (peer.rssi > -80) MeshSignalMedium else MeshSignalWeak
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(CircleShape).background(signalColor.copy(0.15f))) { Icon(imageVector = Icons.Default.Bluetooth, contentDescription = null, tint = signalColor, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(peer.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${peer.nodeId} · ${peer.rssi} dBm", fontSize = 11.sp, color = Color.Gray)
                LinearProgressIndicator(progress = { peer.signalPercent / 100f }, color = signalColor, trackColor = Color.LightGray.copy(0.3f), modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(MeshMintAccent.copy(0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("${peer.hops} Hop", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MeshMintAccent)
            }
        }
    }
}

@Composable
private fun PacketLogCard(log: PacketLogEntry) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${log.sourceName} ➔ ${log.destName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text("[${log.packetType.name}]", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MeshMintAccent)
                }
                Text(log.status, fontSize = 11.sp, color = Color.Gray)
            }
            Text("Hop ${log.hopCount}/${log.maxHops}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        }
    }
}
