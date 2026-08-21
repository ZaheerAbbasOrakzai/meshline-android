package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JoinRequestEntity
import com.example.ui.theme.*

@Composable
fun JoinRequestsDialog(
    pendingRequests: List<JoinRequestEntity>,
    onDismiss: () -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Sensors, null, tint = MeshMintAccent)
            Spacer(Modifier.width(8.dp))
            Text("Nearby Join Requests", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }},
        text = {
            if (pendingRequests.isEmpty()) {
                Text("No pending requests. Nearby nodes will appear here when they try to connect.", fontSize = 14.sp, color = Color.Gray)
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(pendingRequests) { req ->
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(req.peerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Node ID: ${req.nodeId}", fontSize = 12.sp, color = Color.Gray)
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                                    TextButton(onClick = { onRejectRequest(req.id) }) { Text("Ignore", color = Color.Gray) }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = { onAcceptRequest(req.id) }, colors = ButtonDefaults.buttonColors(containerColor = MeshMintAccent)) {
                                        Text("Accept", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
