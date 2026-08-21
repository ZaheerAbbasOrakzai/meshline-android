package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JoinRequestEntity
import com.example.mesh.MeshPeer
import com.example.mesh.MeshRoute
import com.example.mesh.PacketLogEntry
import com.example.ui.theme.MeshGreenPrimary
import com.example.ui.theme.MeshMintAccent
import com.example.ui.theme.MeshSignalMedium
import com.example.ui.theme.MeshSignalStrong
import com.example.ui.theme.MeshSignalWeak

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
    onOpenPairDialog: () -> Unit,
    onTriggerSimulatedBeacon: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "RadarDashboard")
    val pulseScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadarPulse"
    )

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mesh Network Topology",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Decentralized Bluetooth Mesh · Peer-to-Peer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MeshMintAccent
                        )
                    }

                    Row {
                        IconButtonCustom(
                            onClick = onOpenPairDialog,
                            icon = Icons.Default.PersonAdd,
                            desc = "Pair Node"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onStartScan,
                            colors = ButtonDefaults.buttonColors(containerColor = MeshMintAccent),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("scan_ble_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isScanning) "Scanning..." else "Scan BLE",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Pending Join Requests Banner
            if (pendingJoinRequests.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MeshMintAccent.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenJoinRequests() }
                            .testTag("pending_join_requests_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MeshMintAccent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${pendingJoinRequests.size} Pending Mesh Join Request${if (pendingJoinRequests.size > 1) "s" else ""}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Nearby device requesting to connect over BLE",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Button(
                                onClick = onOpenJoinRequests,
                                colors = ButtonDefaults.buttonColors(containerColor = MeshMintAccent),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Review", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // Radar Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MeshGreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(96.dp)
                                .scale(if (isScanning) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(2.dp, MeshMintAccent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "Mesh Hub",
                                tint = MeshMintAccent,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Mesh Status: Active (${peers.size} Verified Peer Links)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Service UUID: a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                            fontSize = 11.sp,
                            color = MeshMintAccent,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats metrics grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatMetric(title = "Max TTL", value = "7 Hops")
                            StatMetric(title = "Avg Latency", value = "< 20ms")
                            StatMetric(title = "Encryption", value = "AES-256")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Relay Mode Toggle Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CellTower,
                                contentDescription = null,
                                tint = MeshMintAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Node Relay Mode",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Forward encrypted packets for multi-hop mesh routing",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Switch(
                            checked = isRelayMode,
                            onCheckedChange = { onToggleRelay() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = MeshMintAccent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Connected Peers Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connected BLE Mesh Peers (${peers.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    OutlinedButton(
                        onClick = onTriggerSimulatedBeacon,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+ Peer Beacon", fontSize = 11.sp, color = MeshMintAccent)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (peers.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Direct Peer Links Connected",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Discover nearby devices or tap '+ Peer Beacon' to test mesh handshakes.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(peers, key = { it.id }) { peer ->
                    PeerNodeCard(peer = peer)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Packet Log Stream Header
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Live Mesh Packet Traffic Feed",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (packetLogs.isEmpty()) {
                item {
                    Text(
                        text = "Awaiting packet transmissions...",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            } else {
                items(packetLogs.take(12)) { log ->
                    PacketLogCard(log = log)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun IconButtonCustom(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
        modifier = Modifier
            .size(38.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = MeshMintAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StatMetric(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 11.sp, color = Color.LightGray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun PeerNodeCard(peer: MeshPeer) {
    val signalColor = when {
        peer.rssi > -60 -> MeshSignalStrong
        peer.rssi > -80 -> MeshSignalMedium
        else -> MeshSignalWeak
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(signalColor.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = signalColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${peer.nodeId} · Signal: ${peer.rssi} dBm (${peer.signalPercent}%)",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { peer.signalPercent / 100f },
                    color = signalColor,
                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MeshMintAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${peer.hops} Hop${if (peer.hops > 1) "s" else ""}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshMintAccent
                )
            }
        }
    }
}

@Composable
private fun PacketLogCard(log: PacketLogEntry) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${log.sourceName} ➔ ${log.destName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[${log.packetType.name}]",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MeshMintAccent
                    )
                }

                Text(
                    text = log.status,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = "Hop ${log.hopCount}/${log.maxHops}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
        }
    }
}
