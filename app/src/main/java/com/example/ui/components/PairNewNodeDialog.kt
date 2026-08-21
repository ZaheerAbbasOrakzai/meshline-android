package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IdentityEntity
import com.example.mesh.MeshPeer
import com.example.ui.theme.*

@Composable
fun PairNewNodeDialog(
    identity: IdentityEntity?,
    discoveredNodes: List<MeshPeer>,
    onDismiss: () -> Unit,
    onSendJoinRequest: (String, String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Mesh Peer", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                Text("Nearby Nodes Detected:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                if (discoveredNodes.isEmpty()) {
                    Text("Scanning for nearby devices... Make sure their Bluetooth is on.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(discoveredNodes) { node ->
                            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(node.name, fontWeight = FontWeight.Bold)
                                        Text(node.nodeId, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Button(onClick = { onSendJoinRequest(node.name, node.nodeId, "") }, colors = ButtonDefaults.buttonColors(containerColor = MeshMintAccent), modifier = Modifier.height(32.dp)) {
                                        Text("Pair", color = Color.Black, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
