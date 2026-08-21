package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ContactEntity
import com.example.ui.theme.MeshGreenPrimary
import com.example.ui.theme.MeshMintAccent
import kotlin.random.Random

@Composable
fun WalkieTalkieDialog(
    contact: ContactEntity?,
    onDismiss: () -> Unit,
    onSendVoiceNote: (durationSec: Int) -> Unit
) {
    if (contact == null) return

    var isTransmitting by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var transmissionSeconds by remember { mutableStateOf(3) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121B22)),
            elevation = CardDefaults.cardElevation(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("walkie_talkie_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MeshMintAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mesh PTT Walkie-Talkie",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        color = if (isTransmitting) Color(0xFFD32F2F) else MeshMintAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isTransmitting) "TRANSMITTING" else "STANDBY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTransmitting) Color.White else MeshMintAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Avatar and Target Contact
                Box(contentAlignment = Alignment.Center) {
                    if (isTransmitting) {
                        Box(
                            modifier = Modifier
                                .size((90 * pulseScale).dp)
                                .clip(CircleShape)
                                .background(MeshMintAccent.copy(alpha = 0.2f))
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MeshGreenPrimary)
                            .border(3.dp, MeshMintAccent, CircleShape)
                    ) {
                        Text(
                            text = contact.initials,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = contact.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Node ID: ${contact.nodeId} · 12ms Hop Latency",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Real-time Audio Waveform Visualizer
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A262F))
                        .padding(horizontal = 16.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val barCount = 32
                    val barWidth = width / barCount

                    for (i in 0 until barCount) {
                        val barHeightFactor = if (isTransmitting) {
                            (0.3f + 0.7f * kotlin.math.sin((i + wavePhase / 20) * 0.5f)).coerceIn(0.1f, 1.0f)
                        } else {
                            0.15f + (i % 3) * 0.05f
                        }

                        val barH = height * barHeightFactor
                        val x = i * barWidth + barWidth / 4

                        drawLine(
                            color = if (isTransmitting) MeshMintAccent else Color.Gray,
                            start = Offset(x, (height - barH) / 2),
                            end = Offset(x, (height + barH) / 2),
                            strokeWidth = barWidth / 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Big PTT Button (Push to talk)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTransmitting) MeshMintAccent else MeshGreenPrimary
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isTransmitting = true
                                    tryAwaitRelease()
                                    isTransmitting = false
                                    onSendVoiceNote(transmissionSeconds)
                                }
                            )
                        }
                        .testTag("push_to_talk_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Push To Talk",
                            tint = if (isTransmitting) Color.Black else Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isTransmitting) "Release" else "HOLD PTT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTransmitting) Color.Black else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Press and hold button to transmit off-grid audio",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Call Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isMuted) Color.DarkGray else Color(0xFF202C33))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Walkie-Talkie",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) MeshGreenPrimary else Color.DarkGray)
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Speaker",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
