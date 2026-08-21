package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactEntity
import com.example.ui.DecryptedMessage
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatConversationScreen(
    contact: ContactEntity?,
    messages: List<DecryptedMessage>,
    onSendMessage: (text: String, hopCount: Int, replyTo: DecryptedMessage?) -> Unit,
    onSendVoiceNote: (durationSec: Int) -> Unit,
    onSendLocation: () -> Unit,
    onAddReaction: (messageId: String, emoji: String) -> Unit,
    onTogglePinMessage: (messageId: String, text: String) -> Unit,
    onSetDisappearingTimer: (seconds: Int) -> Unit,
    onDeleteMessage: (messageId: String) -> Unit,
    onOpenWalkieTalkie: () -> Unit,
    onBack: () -> Unit,
    onToggleSecurityPanel: () -> Unit
) {
    if (contact == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a conversation to begin", color = Color.Gray)
        }
        return
    }

    var messageInput by remember { mutableStateOf("") }
    var selectedHopCount by remember { mutableIntStateOf(1) }
    var showAttachmentsMenu by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }
    var selectedMessageForMenu by remember { mutableStateOf<DecryptedMessage?>(null) }
    var replyingTo by remember { mutableStateOf<DecryptedMessage?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val parseColor = remember(contact.colorHex) {
        try { Color(android.graphics.Color.parseColor(contact.colorHex.replace("0xFF", "#"))) }
        catch (e: Exception) { MeshTealSecondary }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top Bar
        Surface(color = MeshGreenPrimary, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                Box {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(CircleShape).background(parseColor)) {
                        Text(text = contact.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    if (contact.online && contact.id != "saved_messages") {
                        Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(MeshMintAccent).border(2.dp, MeshGreenPrimary, CircleShape).align(Alignment.BottomEnd))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = contact.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        if (contact.verified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.Verified, contentDescription = "Verified", tint = MeshMintAccent, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(text = if (contact.disappearingTimerSec > 0) "Disappearing (${contact.disappearingTimerSec}s)" else "Route healthy", fontSize = 11.sp, color = MeshMintAccent)
                }
                if (contact.id != "saved_messages") {
                    IconButton(onClick = onOpenWalkieTalkie) { Icon(imageVector = Icons.Default.GraphicEq, contentDescription = "Walkie Talkie", tint = MeshMintAccent) }
                }
                Box {
                    IconButton(onClick = { showTimerMenu = !showTimerMenu }) { Icon(imageVector = Icons.Default.Timer, contentDescription = "Disappearing", tint = if (contact.disappearingTimerSec > 0) MeshMintAccent else Color.White) }
                    DropdownMenu(expanded = showTimerMenu, onDismissRequest = { showTimerMenu = false }) {
                        listOf(0 to "Off", 30 to "30s", 300 to "5m", 86400 to "24h").forEach { (sec, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { onSetDisappearingTimer(sec); showTimerMenu = false })
                        }
                    }
                }
                IconButton(onClick = onToggleSecurityPanel) { Icon(imageVector = Icons.Default.Shield, contentDescription = "Security", tint = MeshMintAccent) }
            }
        }

        // Pinned Banner
        if (contact.pinnedMessage != null) {
            Surface(color = MeshDarkSurfaceVariant, modifier = Modifier.fillMaxWidth().clickable { onTogglePinMessage("", "") }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Icon(imageVector = Icons.Default.PushPin, contentDescription = "Pinned", tint = MeshMintAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Pinned Message", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MeshMintAccent)
                        Text(contact.pinnedMessage, fontSize = 12.sp, color = Color.White, maxLines = 1)
                    }
                }
            }
        }

        // List
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(messages, key = { it.entity.id }) { decrypted ->
                MessageBubble(decrypted = decrypted, onSelectMessage = { selectedMessageForMenu = decrypted })
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Action Sheet
        if (selectedMessageForMenu != null) {
            val target = selectedMessageForMenu!!
            Surface(color = MeshDarkSurfaceVariant, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("👍", "❤️", "🔥", "😂", "😮", "🚨").forEach { emoji ->
                            Text(emoji, fontSize = 20.sp, modifier = Modifier.clickable { onAddReaction(target.entity.id, emoji); selectedMessageForMenu = null })
                        }
                    }
                    Row {
                        IconButton(onClick = { replyingTo = target; selectedMessageForMenu = null }) { Icon(Icons.Default.Add, "Reply", tint = MeshMintAccent) }
                        IconButton(onClick = { onTogglePinMessage(target.entity.id, target.plaintext); selectedMessageForMenu = null }) { Icon(Icons.Default.PushPin, "Pin", tint = MeshMintAccent) }
                        IconButton(onClick = { onDeleteMessage(target.entity.id); selectedMessageForMenu = null }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
                    }
                }
            }
        }

        // Reply Preview
        AnimatedVisibility(visible = replyingTo != null) {
            replyingTo?.let { reply ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(40.dp).background(MeshMintAccent, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (reply.entity.senderId == "me") "You" else contact.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MeshMintAccent)
                            Text(reply.plaintext, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { replyingTo = null }) { Icon(Icons.Default.Delete, "Cancel", tint = Color.Gray, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }

        // Composer
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column {
                AnimatedVisibility(visible = showAttachmentsMenu) {
                    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp), Arrangement.SpaceEvenly) {
                        AttachmentIcon(Icons.Default.Mic, "Voice") { onSendVoiceNote(4); showAttachmentsMenu = false }
                        AttachmentIcon(Icons.Default.LocationOn, "GPS") { onSendLocation(); showAttachmentsMenu = false }
                        AttachmentIcon(Icons.Default.Image, "Image") { onSendMessage("📷 Image Attachment", selectedHopCount, null); showAttachmentsMenu = false }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showAttachmentsMenu = !showAttachmentsMenu }) { Icon(Icons.Default.Add, null, tint = MeshMintAccent) }
                    OutlinedTextField(
                        value = messageInput, onValueChange = { messageInput = it },
                        placeholder = { Text("Type message...") }, maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MeshMintAccent),
                        shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { if (messageInput.isNotBlank()) { onSendMessage(messageInput, selectedHopCount, replyingTo); messageInput = ""; replyingTo = null } else onSendVoiceNote(3) },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MeshMintAccent)
                    ) {
                        Icon(imageVector = if (messageInput.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic, contentDescription = "Send", tint = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Icon(icon, null, tint = MeshMintAccent)
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun MessageBubble(decrypted: DecryptedMessage, onSelectMessage: () -> Unit) {
    val isMe = decrypted.entity.senderId == "me"
    val isSystem = decrypted.entity.kind == "system"
    if (isSystem) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), Alignment.Center) {
            Surface(color = Color.DarkGray.copy(0.5f), shape = RoundedCornerShape(16.dp)) {
                Text(decrypted.plaintext, fontSize = 11.sp, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
            }
        }
        return
    }

    val bubbleShape = if (isMe) RoundedCornerShape(15.dp, 5.dp, 15.dp, 15.dp) else RoundedCornerShape(5.dp, 15.dp, 15.dp, 15.dp)
    val bgColor = if (isMe) MeshDarkBubble else MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
        Card(shape = bubbleShape, colors = CardDefaults.cardColors(containerColor = bgColor), modifier = Modifier.widthIn(max = 280.dp).clickable(onClick = onSelectMessage)) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (decrypted.entity.replyToId != null) {
                    Surface(color = Color.Black.copy(0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            Box(modifier = Modifier.width(3.dp).height(30.dp).background(MeshMintAccent))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(decrypted.entity.replyToSender ?: "Peer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MeshMintAccent)
                                Text(decrypted.entity.replyToText ?: "", fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (decrypted.entity.isPinned) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PushPin, null, tint = MeshMintAccent, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)); Text("Pinned", fontSize = 10.sp, color = MeshMintAccent) }
                }
                when (decrypted.entity.kind) {
                    "voice" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, null, tint = MeshMintAccent)
                            Spacer(Modifier.width(8.dp))
                            Text(decrypted.plaintext, fontSize = 14.sp, color = textColor)
                        }
                    }
                    "location" -> {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.DarkGray.copy(0.3f)), Alignment.Center) { Icon(Icons.Default.LocationOn, null, tint = MeshMintAccent, modifier = Modifier.size(32.dp)) }
                            Text(decrypted.plaintext, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                    else -> Text(decrypted.plaintext, fontSize = 14.sp, color = textColor)
                }
                if (decrypted.entity.reactions.isNotBlank()) {
                    Text(decrypted.entity.reactions, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp).background(Color.Black.copy(0.1f)).padding(2.dp))
                }
                Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Text("Hop ${decrypted.entity.hopCount}", fontSize = 10.sp, color = Color.Gray)
                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        Icon(imageVector = if (decrypted.entity.status == "read") Icons.Default.DoneAll else Icons.Default.Check, contentDescription = null, tint = if (decrypted.entity.status == "read") MeshMintAccent else Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
