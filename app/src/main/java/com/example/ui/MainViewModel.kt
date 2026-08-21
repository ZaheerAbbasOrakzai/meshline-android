package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.MeshDatabase
import com.example.data.model.ContactEntity
import com.example.data.model.IdentityEntity
import com.example.data.model.JoinRequestEntity
import com.example.data.model.MessageEntity
import com.example.data.repository.MeshRepository
import com.example.mesh.MeshEngine
import com.example.mesh.MeshPeer
import com.example.mesh.MeshRoute
import com.example.mesh.PacketLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    CHATS,
    NETWORK,
    CONTACTS,
    SETTINGS
}

data class DecryptedMessage(
    val entity: MessageEntity,
    val plaintext: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MeshDatabase.getInstance(application)
    private val meshEngine = MeshEngine(application)
    private val repository = MeshRepository(
        identityDao = database.identityDao(),
        contactDao = database.contactDao(),
        messageDao = database.messageDao(),
        joinRequestDao = database.joinRequestDao(),
        meshEngine = meshEngine
    )

    val identity: StateFlow<IdentityEntity?> = repository.identityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val contacts: StateFlow<List<ContactEntity>> = repository.contactsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val joinRequests: StateFlow<List<JoinRequestEntity>> = repository.joinRequestsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingJoinRequests: StateFlow<List<JoinRequestEntity>> = repository.pendingJoinRequestsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userPassphrase = MutableStateFlow("MeshlineMasterPass123!")
    val userPassphrase: StateFlow<String> = _userPassphrase.asStateFlow()

    private val _activeTab = MutableStateFlow(MainTab.CHATS)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _selectedConversationId = MutableStateFlow<String?>("public_channel")
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    private val _showSecurityPanel = MutableStateFlow(false)
    val showSecurityPanel: StateFlow<Boolean> = _showSecurityPanel.asStateFlow()

    private val _showPanicDialog = MutableStateFlow(false)
    val showPanicDialog: StateFlow<Boolean> = _showPanicDialog.asStateFlow()

    private val _showJoinRequestsDialog = MutableStateFlow(false)
    val showJoinRequestsDialog: StateFlow<Boolean> = _showJoinRequestsDialog.asStateFlow()

    private val _showPairNewNodeDialog = MutableStateFlow(false)
    val showPairNewNodeDialog: StateFlow<Boolean> = _showPairNewNodeDialog.asStateFlow()

    val meshPeers: StateFlow<List<MeshPeer>> = meshEngine.peers
    val discoveredNearbyNodes: StateFlow<List<MeshPeer>> = meshEngine.discoveredNearbyNodes
    val meshRoutes: StateFlow<List<MeshRoute>> = meshEngine.routes
    val packetLogs: StateFlow<List<PacketLogEntry>> = meshEngine.packetLogs
    val isScanning: StateFlow<Boolean> = meshEngine.isScanning
    val isRelayMode: StateFlow<Boolean> = meshEngine.isRelayEnabled

    init {
        viewModelScope.launch {
            contacts.collect { contactList ->
                meshEngine.syncPeersWithContacts(contactList)
            }
        }
    }

    val activeMessages: StateFlow<List<DecryptedMessage>> = combine(
        _selectedConversationId,
        contacts,
        _userPassphrase
    ) { convId, contactList, pass ->
        Triple(convId, contactList, pass)
    }.flatMapLatest { (convId, contactList, pass) ->
        if (convId == null) return@flatMapLatest flowOf(emptyList())
        val contact = contactList.find { it.id == convId } ?: return@flatMapLatest flowOf(emptyList())

        repository.getMessagesFlow(convId).combine(flowOf(contact)) { rawMsgs, targetContact ->
            rawMsgs.map { msg ->
                val text = repository.decryptMessageContent(msg, targetContact.publicKey, pass)
                DecryptedMessage(entity = msg, plaintext = text)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPassphrase(pass: String) {
        _userPassphrase.value = pass
    }

    fun setActiveTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun selectConversation(id: String?) {
        _selectedConversationId.value = id
    }

    fun createIdentity(username: String, passphrase: String) {
        viewModelScope.launch {
            _userPassphrase.value = passphrase
            repository.createIdentity(username, passphrase)
            _selectedConversationId.value = "public_channel"
        }
    }

    private val _showWalkieTalkie = MutableStateFlow(false)
    val showWalkieTalkie: StateFlow<Boolean> = _showWalkieTalkie.asStateFlow()

    private val _showCreateGroupDialog = MutableStateFlow(false)
    val showCreateGroupDialog: StateFlow<Boolean> = _showCreateGroupDialog.asStateFlow()

    fun toggleWalkieTalkie() {
        _showWalkieTalkie.value = !_showWalkieTalkie.value
    }

    fun toggleCreateGroupDialog() {
        _showCreateGroupDialog.value = !_showCreateGroupDialog.value
    }

    fun toggleJoinRequestsDialog() {
        _showJoinRequestsDialog.value = !_showJoinRequestsDialog.value
    }

    fun togglePairNewNodeDialog() {
        _showPairNewNodeDialog.value = !_showPairNewNodeDialog.value
    }

    fun sendJoinRequest(peerName: String, peerNodeId: String, peerPublicKey: String) {
        viewModelScope.launch {
            repository.sendJoinRequest(peerName, peerNodeId, peerPublicKey)
        }
    }

    fun acceptJoinRequest(requestId: String) {
        viewModelScope.launch {
            val contact = repository.acceptJoinRequest(requestId, _userPassphrase.value)
            if (contact != null) {
                _selectedConversationId.value = contact.id
                _activeTab.value = MainTab.CHATS
                _showJoinRequestsDialog.value = false
                _showPairNewNodeDialog.value = false
            }
        }
    }

    fun rejectJoinRequest(requestId: String) {
        viewModelScope.launch {
            repository.rejectJoinRequest(requestId)
        }
    }

    fun triggerSimulatedBeacon(customName: String? = null, customNodeId: String? = null) {
        viewModelScope.launch {
            repository.triggerSimulatedPeerBeacon(customName, customNodeId)
        }
    }

    fun createGroupChannel(groupName: String, membersCount: Int) {
        viewModelScope.launch {
            val newGroupId = repository.createGroupChannel(groupName, membersCount)
            _selectedConversationId.value = newGroupId
            _showCreateGroupDialog.value = false
        }
    }

    fun sendMessage(text: String, hopCount: Int = 1, replyTo: DecryptedMessage? = null) {
        val convId = _selectedConversationId.value ?: return
        if (text.isBlank()) return
        
        val contact = contacts.value.find { it.id == convId }
        val replySender = if (replyTo != null) {
            if (replyTo.entity.senderId == "me") "You" else contact?.name ?: "Mesh Peer"
        } else null

        viewModelScope.launch {
            repository.sendMessage(
                conversationId = convId,
                text = text,
                passphrase = _userPassphrase.value,
                hopCount = hopCount,
                kind = "text",
                replyToId = replyTo?.entity?.id,
                replyToText = replyTo?.plaintext,
                replyToSender = replySender
            )
        }
    }

    fun sendVoiceNote(durationSec: Int) {
        val convId = _selectedConversationId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = convId,
                text = "🎤 Voice Note ($durationSec sec)",
                passphrase = _userPassphrase.value,
                kind = "voice",
                mediaUriOrData = "duration:$durationSec"
            )
        }
    }

    fun sendLocationMessage(label: String = "Current Coordinates", lat: Double = 37.7749, lon: Double = -122.4194) {
        val convId = _selectedConversationId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = convId,
                text = "📍 Mesh GPS: $lat, $lon ($label)",
                passphrase = _userPassphrase.value,
                kind = "location",
                mediaUriOrData = "$lat,$lon"
            )
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.addMessageReaction(messageId, emoji)
        }
    }

    fun togglePinMessage(messageId: String, plaintext: String) {
        val convId = _selectedConversationId.value ?: return
        viewModelScope.launch {
            repository.togglePinMessage(convId, messageId, plaintext)
        }
    }

    fun setDisappearingTimer(timerSec: Int) {
        val convId = _selectedConversationId.value ?: return
        viewModelScope.launch {
            repository.setDisappearingTimer(convId, timerSec)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun toggleSecurityPanel() {
        _showSecurityPanel.value = !_showSecurityPanel.value
    }

    fun togglePanicDialog() {
        _showPanicDialog.value = !_showPanicDialog.value
    }

    fun confirmPanicWipe() {
        viewModelScope.launch {
            repository.panicWipeData()
            _showPanicDialog.value = false
            _selectedConversationId.value = null
        }
    }

    fun startBleScan() {
        meshEngine.startBleDiscovery()
    }

    fun toggleRelayMode() {
        meshEngine.toggleRelayMode()
    }

    fun setMeshService(service: com.example.mesh.MeshService) {
        meshEngine.setService(service)
        service.startMesh()
    }

    fun toggleContactVerification(contactId: String) {
        viewModelScope.launch {
            val contact = contacts.value.find { it.id == contactId } ?: return@launch
            repository.updateContactVerification(contactId, !contact.verified)
        }
    }

    fun getIdentityExportString(): String? {
        val id = identity.value ?: return null
        // Format: version|username|pubKey|encryptedPrivKey|salt
        return "1|${id.username}|${id.publicKey}|${id.encryptedPrivateKey}|${id.saltBase64}"
    }

    fun importIdentity(exportString: String) {
        viewModelScope.launch {
            try {
                val parts = exportString.split("|")
                if (parts.size != 5) return@launch
                val username = parts[1]
                val pubKey = parts[2]
                val encPrivKey = parts[3]
                val salt = parts[4]
                
                val imported = com.example.data.model.IdentityEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    username = username,
                    avatar = username.take(2).uppercase(),
                    publicKey = pubKey,
                    encryptedPrivateKey = encPrivKey,
                    saltBase64 = salt
                )
                repository.importIdentity(imported)
            } catch (e: Exception) {}
        }
    }
}
