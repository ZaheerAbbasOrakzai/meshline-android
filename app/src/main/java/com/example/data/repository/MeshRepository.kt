package com.example.data.repository

import com.example.crypto.CryptoEngine
import com.example.data.dao.ContactDao
import com.example.data.dao.IdentityDao
import com.example.data.dao.JoinRequestDao
import com.example.data.dao.MessageDao
import com.example.data.model.ContactEntity
import com.example.data.model.IdentityEntity
import com.example.data.model.JoinRequestEntity
import com.example.data.model.MessageEntity
import com.example.mesh.MeshEngine
import com.example.mesh.MeshPacket
import com.example.mesh.MeshPeer
import com.example.mesh.PacketType
import com.example.mesh.PeerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class MeshRepository(
    private val identityDao: IdentityDao,
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
    private val joinRequestDao: JoinRequestDao,
    val meshEngine: MeshEngine
) {

    val identityFlow: Flow<IdentityEntity?> = identityDao.getIdentityFlow()
    val contactsFlow: Flow<List<ContactEntity>> = contactDao.getAllContactsFlow()
    val joinRequestsFlow: Flow<List<JoinRequestEntity>> = joinRequestDao.getAllRequestsFlow()
    val pendingJoinRequestsFlow: Flow<List<JoinRequestEntity>> = joinRequestDao.getPendingRequestsFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        repositoryScope.launch {
            identityFlow.collect { id ->
                if (id != null) {
                    meshEngine.setLocalNodeId(id.id)
                }
            }
        }

        meshEngine.setOnPacketReceivedCallback { packet ->
            repositoryScope.launch {
                handleIncomingPacket(packet)
            }
        }

        repositoryScope.launch {
            while (true) {
                messageDao.deleteExpiredMessages(System.currentTimeMillis())
                delay(10000) // Every 10 seconds
            }
        }
    }

    private suspend fun handleIncomingPacket(packet: MeshPacket) {
        if (packet.packetType == PacketType.ACK) {
            messageDao.updateMessageStatus(packet.payload, "delivered")
            delay(500)
            messageDao.updateMessageStatus(packet.payload, "read")
            return
        }

        val contact = contactDao.getContactByNodeId(packet.sourceId) ?: return
        
        val message = MessageEntity(
            id = packet.messageId,
            conversationId = contact.id,
            senderId = contact.id,
            encryptedContent = packet.payload,
            timestamp = System.currentTimeMillis(),
            status = "delivered",
            kind = if (packet.packetType == PacketType.DISCOVERY) "system" else "text",
            hopCount = packet.hopCount,
            expiresAt = if (contact.disappearingTimerSec > 0) System.currentTimeMillis() + (contact.disappearingTimerSec * 1000L) else null
        )
        
        messageDao.insertMessage(message)

        val identity = identityDao.getIdentity()
        if (identity != null && packet.packetType == PacketType.DATA) {
            val ackPacket = MeshPacket(
                sourceId = identity.id,
                destId = packet.sourceId,
                messageId = UUID.randomUUID().toString(),
                packetType = PacketType.ACK,
                payload = packet.messageId
            )
            meshEngine.routePacket(ackPacket, identity.username, contact.name)
        }
    }

    fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>> = 
        messageDao.getMessagesForConversationFlow(conversationId)

    suspend fun createIdentity(username: String, passphrase: String): IdentityEntity {
        val salt = CryptoEngine.generateSalt()
        val masterKey = CryptoEngine.deriveMasterKey(passphrase, salt)
        val keyPair = CryptoEngine.generateEcdhKeyPair()
        val pubKeyStr = CryptoEngine.encodePublicKey(keyPair.public)
        val privKeyStr = CryptoEngine.encodePrivateKey(keyPair.private)
        val encryptedPrivKey = CryptoEngine.encryptWithKey(privKeyStr, masterKey)
        val saltBase64 = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)

        val identity = IdentityEntity(
            id = UUID.randomUUID().toString(),
            username = username,
            avatar = username.take(2).uppercase(),
            publicKey = pubKeyStr,
            encryptedPrivateKey = encryptedPrivKey,
            saltBase64 = saltBase64
        )

        identityDao.insertIdentity(identity)
        seedInitialVaultAndPublicChannel(identity)
        return identity
    }

    private suspend fun seedInitialVaultAndPublicChannel(identity: IdentityEntity) {
        if (contactDao.getAllContactsFlow().firstOrNull()?.isNotEmpty() == true) return

        val publicChannel = ContactEntity(
            id = "public_channel",
            name = "Public Mesh Channel",
            nodeId = "broadcast_all",
            initials = "MESH",
            colorHex = "0xFF006C4C",
            verified = true,
            online = true,
            lastSeen = "Open Mesh Broadcast",
            publicKey = identity.publicKey,
            isGroup = true
        )

        val savedMessages = ContactEntity(
            id = "saved_messages",
            name = "Saved Messages",
            nodeId = "self_vault",
            initials = "VAULT",
            colorHex = "0xFF386A20",
            verified = true,
            online = true,
            lastSeen = "Encrypted Local Storage",
            publicKey = identity.publicKey,
            isGroup = false
        )

        contactDao.insertContact(publicChannel)
        contactDao.insertContact(savedMessages)
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        passphrase: String,
        hopCount: Int = 1,
        kind: String = "text",
        mediaUriOrData: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSender: String? = null
    ) {
        val identity = identityDao.getIdentity() ?: return
        val contact = contactDao.getContactById(conversationId) ?: return

        val salt = android.util.Base64.decode(identity.saltBase64, android.util.Base64.DEFAULT)
        val masterKey = CryptoEngine.deriveMasterKey(passphrase, salt)

        val cipherPayload = if (conversationId == "public_channel") {
            "[PUBLIC BROADCAST] $text"
        } else if (conversationId == "saved_messages") {
            CryptoEngine.encryptWithKey(text, masterKey)
        } else {
            val privKeyStr = CryptoEngine.decryptWithKey(identity.encryptedPrivateKey, masterKey)
            val myPrivKey = CryptoEngine.decodePrivateKey(privKeyStr)
            val peerPubKey = CryptoEngine.decodePublicKey(contact.publicKey)
            CryptoEngine.encryptMessage(text, myPrivKey, peerPubKey)
        }

        val msgId = UUID.randomUUID().toString()
        val message = MessageEntity(
            id = msgId,
            conversationId = conversationId,
            senderId = "me",
            encryptedContent = cipherPayload,
            timestamp = System.currentTimeMillis(),
            status = "sent",
            kind = kind,
            mediaUriOrData = mediaUriOrData,
            hopCount = hopCount,
            replyToId = replyToId,
            replyToText = replyToText,
            replyToSender = replyToSender,
            expiresAt = if (contact.disappearingTimerSec > 0) System.currentTimeMillis() + (contact.disappearingTimerSec * 1000L) else null
        )

        messageDao.insertMessage(message)

        if (conversationId != "saved_messages") {
            val packet = MeshPacket(
                sourceId = identity.id,
                destId = contact.nodeId,
                messageId = msgId,
                hopCount = hopCount,
                packetType = if (conversationId == "public_channel") PacketType.DISCOVERY else PacketType.DATA,
                payload = cipherPayload
            )
            meshEngine.routePacket(packet, identity.username, contact.name)
        }
    }

    suspend fun addMessageReaction(messageId: String, emoji: String) {
        val allMsgs = messageDao.getAllMessagesFlow().firstOrNull() ?: return
        val target = allMsgs.find { it.id == messageId } ?: return
        val updated = if (target.reactions.contains(emoji)) target.reactions.replace(emoji, "").trim(',') else if (target.reactions.isBlank()) emoji else "${target.reactions}, $emoji"
        messageDao.updateMessageReactions(messageId, updated)
    }

    suspend fun togglePinMessage(contactId: String, messageId: String, contentText: String) {
        if (messageId.isBlank()) {
            contactDao.updateContactPinnedMessage(contactId, null)
            return
        }
        val target = messageDao.getMessagesForConversationFlow(contactId).firstOrNull()?.find { it.id == messageId } ?: return
        val newState = !target.isPinned
        messageDao.updateMessagePinned(messageId, newState)
        contactDao.updateContactPinnedMessage(contactId, if (newState) contentText else null)
    }

    suspend fun setDisappearingTimer(contactId: String, timerSeconds: Int) = contactDao.updateContactDisappearingTimer(contactId, timerSeconds)
    suspend fun deleteMessage(messageId: String) = messageDao.deleteMessageById(messageId)

    suspend fun createGroupChannel(groupName: String, membersCount: Int): String {
        val groupId = "group_" + UUID.randomUUID().toString().take(8)
        val group = ContactEntity(
            id = groupId,
            name = groupName,
            nodeId = "group_relay_$groupId",
            initials = groupName.take(2).uppercase(),
            colorHex = "0xFF7D5260",
            verified = true,
            online = true,
            lastSeen = "Group Mesh Channel",
            publicKey = UUID.randomUUID().toString(), // Dummy for group
            isGroup = true,
            memberCount = membersCount
        )
        contactDao.insertContact(group)
        return groupId
    }

    suspend fun decryptMessageContent(message: MessageEntity, contactPublicKey: String, passphrase: String): String {
        if (message.encryptedContent.startsWith("[PUBLIC BROADCAST]")) return message.encryptedContent.removePrefix("[PUBLIC BROADCAST]").trim()
        val identity = identityDao.getIdentity() ?: return "Encrypted"
        return try {
            val salt = android.util.Base64.decode(identity.saltBase64, android.util.Base64.DEFAULT)
            val masterKey = CryptoEngine.deriveMasterKey(passphrase, salt)
            
            if (message.conversationId == "saved_messages") {
                return CryptoEngine.decryptWithKey(message.encryptedContent, masterKey)
            }
            
            val privKeyStr = CryptoEngine.decryptWithKey(identity.encryptedPrivateKey, masterKey)
            val myPrivKey = CryptoEngine.decodePrivateKey(privKeyStr)
            val peerPubKey = CryptoEngine.decodePublicKey(contactPublicKey)
            CryptoEngine.decryptMessage(message.encryptedContent, myPrivKey, peerPubKey)
        } catch (e: Exception) {
            "Decryption Error"
        }
    }

    suspend fun updateContactVerification(contactId: String, isVerified: Boolean) {
        contactDao.getContactById(contactId)?.let { contactDao.updateContact(it.copy(verified = isVerified)) }
    }

    suspend fun sendJoinRequest(peerName: String, peerNodeId: String, peerPublicKey: String) {
        val request = JoinRequestEntity(id = UUID.randomUUID().toString(), peerName = peerName, nodeId = peerNodeId, publicKey = peerPublicKey, status = "PENDING", direction = "OUTBOUND")
        joinRequestDao.insertRequest(request)
        repositoryScope.launch { delay(1500); acceptJoinRequest(request.id, "") }
    }

    suspend fun acceptJoinRequest(requestId: String, passphrase: String): ContactEntity? {
        val request = joinRequestDao.getRequestById(requestId) ?: return null
        joinRequestDao.updateRequest(request.copy(status = "ACCEPTED"))
        val contact = ContactEntity(id = "contact_${request.nodeId}", name = request.peerName, nodeId = request.nodeId, initials = request.peerName.take(2).uppercase(), colorHex = "0xFF006C4C", verified = true, online = true, publicKey = request.publicKey)
        contactDao.insertContact(contact)
        meshEngine.registerConnectedPeer(MeshPeer(contact.id, contact.name, contact.nodeId, request.rssi, request.hops, PeerState.CONNECTED, contact.name.contains("Relay")))
        return contact
    }

    suspend fun rejectJoinRequest(requestId: String) {
        joinRequestDao.getRequestById(requestId)?.let { joinRequestDao.updateRequest(it.copy(status = "REJECTED")) }
    }

    suspend fun triggerSimulatedPeerBeacon(name: String? = null, id: String? = null) {
        val peerName = name ?: listOf("Elena Rostova", "Marcus Relay", "Dr. Thorne").random()
        val nodeId = id ?: "node_${(1000..9999).random()}"
        val request = JoinRequestEntity(id = UUID.randomUUID().toString(), peerName = peerName, nodeId = nodeId, publicKey = UUID.randomUUID().toString(), status = "PENDING", direction = "INBOUND")
        joinRequestDao.insertRequest(request)
        meshEngine.addDiscoveredNode(MeshPeer("disc_$nodeId", peerName, nodeId, -60, 1, PeerState.DISCOVERING, peerName.contains("Relay")))
    }

    suspend fun panicWipeData() {
        identityDao.clearIdentity(); contactDao.clearContacts(); messageDao.clearMessages(); joinRequestDao.clearRequests(); meshEngine.clearAll()
    }

    suspend fun importIdentity(identity: IdentityEntity) {
        identityDao.clearIdentity()
        identityDao.insertIdentity(identity)
        seedInitialVaultAndPublicChannel(identity)
    }
}
