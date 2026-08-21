package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactEntity
import com.example.ui.theme.*

@Composable
fun ChatsListScreen(
    contacts: List<ContactEntity>,
    selectedContactId: String?,
    onSelectContact: (String) -> Unit,
    onStartScan: () -> Unit,
    onOpenCreateGroup: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredContacts = remember(contacts, searchQuery, selectedFilter) {
        contacts.filter { contact ->
            val matchesSearch = searchQuery.isBlank() || contact.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Groups" -> contact.isGroup || contact.id == "public_channel"
                "Saved" -> contact.id == "saved_messages"
                "Direct" -> !contact.isGroup && contact.id != "public_channel" && contact.id != "saved_messages"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = onOpenCreateGroup, containerColor = MeshGreenPrimary, contentColor = Color.White, shape = CircleShape, modifier = Modifier.padding(bottom = 12.dp)) {
                    Icon(Icons.Default.GroupAdd, "New Group")
                }
                FloatingActionButton(onClick = onStartScan, containerColor = MeshMintAccent, contentColor = Color.Black, shape = CircleShape) {
                    Icon(Icons.Default.BluetoothSearching, "Scan Mesh")
                }
            }
        }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)) {
            Surface(color = MeshGreenPrimary, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text("Meshline", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, null, tint = MeshMintAccent, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("End-to-End Encrypted Mesh", fontSize = 11.sp, color = MeshMintAccent)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        placeholder = { Text("Search nodes or channels...", color = Color.LightGray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.LightGray) },
                        singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MeshMintAccent, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All", "Direct", "Groups", "Saved")) { filter ->
                            val isSelected = selectedFilter == filter
                            FilterChip(
                                selected = isSelected, onClick = { selectedFilter = filter },
                                label = { Text(filter, fontSize = 12.sp, color = if (isSelected) Color.Black else Color.White) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MeshMintAccent, containerColor = Color.White.copy(0.15f)),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            if (filteredContacts.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BluetoothSearching, null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No mesh nodes found. Tap Scan Mesh to start.", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        ConversationRow(contact, contact.id == selectedContactId) { onSelectContact(contact.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(contact: ContactEntity, isSelected: Boolean, onClick: () -> Unit) {
    val parseColor = try { Color(android.graphics.Color.parseColor(contact.colorHex.replace("0xFF", "#"))) } catch (e: Exception) { MeshTealSecondary }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(0.5f) else Color.Transparent).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp).clip(CircleShape).background(parseColor)) {
                if (contact.id == "saved_messages") Icon(Icons.Default.Bookmark, null, tint = Color.White)
                else if (contact.isGroup) Icon(Icons.Default.Group, null, tint = Color.White)
                else Text(contact.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            if (contact.online && contact.id != "saved_messages") {
                Box(Modifier.size(14.dp).clip(CircleShape).background(MeshMintAccent).border(2.dp, MeshDarkBackground, CircleShape).align(Alignment.BottomEnd))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(contact.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (contact.verified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, null, tint = MeshMintAccent, modifier = Modifier.size(16.dp))
                    }
                }
                Text(if (contact.online) "Active" else contact.lastSeen, fontSize = 11.sp, color = if (contact.online) MeshMintAccent else Color.Gray)
            }
            Spacer(Modifier.height(4.dp))
            Text(contact.pinnedMessage ?: if (contact.isGroup) "Group Channel" else "Node ID: ${contact.nodeId}", fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
