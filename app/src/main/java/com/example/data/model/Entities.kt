package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identity")
data class IdentityEntity(
    @PrimaryKey val id: String,
    val username: String,
    val avatar: String,
    val publicKey: String,
    val encryptedPrivateKey: String,
    val saltBase64: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nodeId: String,
    val initials: String,
    val colorHex: String,
    val verified: Boolean = false,
    val online: Boolean = true,
    val lastSeen: String = "Active now",
    val publicKey: String,
    val isGroup: Boolean = false,
    val memberCount: Int = 1,
    val pinnedMessage: String? = null,
    val disappearingTimerSec: Int = 0
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val encryptedContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sent", // sent, delivered, read
    val kind: String = "text", // text, image, voice, location, system
    val mediaUriOrData: String? = null,
    val reactions: String = "",
    val isStarred: Boolean = false,
    val isPinned: Boolean = false,
    val hopCount: Int = 1,
    val maxHops: Int = 7,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val expiresAt: Long? = null
)

@Entity(tableName = "join_requests")
data class JoinRequestEntity(
    @PrimaryKey val id: String,
    val peerName: String,
    val nodeId: String,
    val publicKey: String,
    val rssi: Int = -60,
    val hops: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val direction: String = "INBOUND" // INBOUND, OUTBOUND
)

