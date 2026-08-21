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

enum class MainTab { CHATS, NETWORK, CONTACTS, SETTINGS }
data class DecryptedMessage(val entity: MessageEntity, val plaintext: String)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MeshDatabase.getInstance(application)
    private val meshEngine = MeshEngine(application)
    private val repository = MeshRepository(database.identityDao(), database.contactDao(), database.messageDao(), database.joinRequestDao(), meshEngine)

    val identity = repository.identityFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val contacts = repository.contactsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val joinRequests = repository.joinRequestsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingJoinRequests = repository.pendingJoinRequestsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userPassphrase = MutableStateFlow("MeshlineMasterPass123!")
    private val _activeTab = MutableStateFlow(MainTab.CHATS)
    private val _selectedConversationId = MutableStateFlow<String?>("public_channel")
    private val _showSecurityPanel = MutableStateFlow(false)
    private val _showPanicDialog = MutableStateFlow(false)
    private val _showJoinRequestsDialog = MutableStateFlow(false)
    private val _showPairNewNodeDialog = MutableStateFlow(false)
    private val _requestBluetoothEnable = MutableStateFlow(false)

    val userPassphrase = _userPassphrase.asStateFlow()
    val activeTab = _activeTab.asStateFlow()
    val selectedConversationId = _selectedConversationId.asStateFlow()
    val showSecurityPanel = _showSecurityPanel.asStateFlow()
    val showPanicDialog = _showPanicDialog.asStateFlow()
    val showJoinRequestsDialog = _showJoinRequestsDialog.asStateFlow()
    val showPairNewNodeDialog = _showPairNewNodeDialog.asStateFlow()
    val requestBluetoothEnable = _requestBluetoothEnable.asStateFlow()

    val meshPeers = meshEngine.peers
    val discoveredNearbyNodes = meshEngine.discoveredNearbyNodes
    val meshRoutes = meshEngine.routes
    val packetLogs = meshEngine.packetLogs
    val isScanning = meshEngine.isScanning
    val isRelayMode = meshEngine.isRelayEnabled

    init {
        viewModelScope.launch { contacts.collect { meshEngine.syncPeersWithContacts(it) } }
        meshEngine.setOnBluetoothRequiredCallback { _requestBluetoothEnable.value = true }
    }

    val activeMessages = combine(_selectedConversationId, contacts, _userPassphrase) { id, list, pass -> Triple(id, list, pass) }
        .flatMapLatest { (id, list, pass) ->
            if (id == null) return@flatMapLatest flowOf(emptyList())
            val contact = list.find { it.id == id } ?: return@flatMapLatest flowOf(emptyList())
            repository.getMessagesFlow(id).combine(flowOf(contact)) { msgs, c -> msgs.map { DecryptedMessage(it, repository.decryptMessageContent(it, c.publicKey, pass)) } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPassphrase(pass: String) { _userPassphrase.value = pass }
    fun setActiveTab(tab: MainTab) { _activeTab.value = tab }
    fun selectConversation(id: String?) { _selectedConversationId.value = id }
    fun bluetoothEnableHandled() { _requestBluetoothEnable.value = false }
    fun toggleSecurityPanel() { _showSecurityPanel.value = !_showSecurityPanel.value }
    fun togglePanicDialog() { _showPanicDialog.value = !_showPanicDialog.value }
    fun toggleJoinRequestsDialog() { _showJoinRequestsDialog.value = !_showJoinRequestsDialog.value }
    fun togglePairNewNodeDialog() { _showPairNewNodeDialog.value = !_showPairNewNodeDialog.value }
    fun toggleRelayMode() { meshEngine.toggleRelayMode() }
    fun startBleScan() { meshEngine.startBleDiscovery() }

    fun setMeshService(service: com.example.mesh.MeshService) {
        meshEngine.setService(service)
        service.startMesh()
    }

    fun createIdentity(u: String, p: String) { viewModelScope.launch { _userPassphrase.value = p; repository.createIdentity(u, p); _selectedConversationId.value = "public_channel" } }
    fun updateProfile(u: String, a: String) { viewModelScope.launch { repository.updateProfile(u, a) } }
    fun sendJoinRequest(n: String, i: String, k: String) { viewModelScope.launch { repository.sendJoinRequest(n, i, k) } }
    fun acceptJoinRequest(id: String) { viewModelScope.launch { repository.acceptJoinRequest(id, _userPassphrase.value)?.let { _selectedConversationId.value = it.id; _activeTab.value = MainTab.CHATS; _showJoinRequestsDialog.value = false; _showPairNewNodeDialog.value = false } } }
    fun rejectJoinRequest(id: String) { viewModelScope.launch { repository.rejectJoinRequest(id) } }
    fun createGroupChannel(n: String, m: Int) { viewModelScope.launch { _selectedConversationId.value = repository.createGroupChannel(n, m); _showCreateGroupDialog.value = false } }

    fun sendMessage(t: String, h: Int = 1, r: DecryptedMessage? = null) {
        val cid = _selectedConversationId.value ?: return
        if (t.isBlank()) return
        val c = contacts.value.find { it.id == cid }
        val rs = if (r != null) (if (r.entity.senderId == "me") "You" else c?.name ?: "Peer") else null
        viewModelScope.launch { repository.sendMessage(cid, t, _userPassphrase.value, h, "text", null, r?.entity?.id, r?.plaintext, rs) }
    }

    fun sendVoiceNote(d: Int) { val cid = _selectedConversationId.value ?: return; viewModelScope.launch { repository.sendMessage(cid, "🎤 Voice ($d s)", _userPassphrase.value, 1, "voice", "duration:$d") } }
    fun sendLocationMessage() { val cid = _selectedConversationId.value ?: return; viewModelScope.launch { repository.sendMessage(cid, "📍 GPS Location", _userPassphrase.value, 1, "location", "37.7,-122.4") } }
    fun addReaction(id: String, e: String) { viewModelScope.launch { repository.addMessageReaction(id, e) } }
    fun togglePinMessage(id: String, t: String) { val cid = _selectedConversationId.value ?: return; viewModelScope.launch { repository.togglePinMessage(cid, id, t) } }
    fun setDisappearingTimer(s: Int) { val cid = _selectedConversationId.value ?: return; viewModelScope.launch { repository.setDisappearingTimer(cid, s) } }
    fun deleteMessage(id: String) { viewModelScope.launch { repository.deleteMessage(id) } }
    fun confirmPanicWipe() { viewModelScope.launch { repository.panicWipeData(); _showPanicDialog.value = false; _selectedConversationId.value = null } }
    fun toggleContactVerification(id: String) { viewModelScope.launch { contacts.value.find { it.id == id }?.let { repository.updateContactVerification(id, !it.verified) } } }
    fun getIdentityExportString() = identity.value?.let { "1|${it.username}|${it.publicKey}|${it.encryptedPrivateKey}|${it.saltBase64}" }
    fun importIdentity(s: String) { viewModelScope.launch { try { val p = s.split("|"); if (p.size == 5) repository.importIdentity(com.example.data.model.IdentityEntity(java.util.UUID.randomUUID().toString(), p[1], p[1].take(2).uppercase(), p[2], p[3], p[4])) } catch (e: Exception) {} } }

    private val _showCreateGroupDialog = MutableStateFlow(false)
    val showCreateGroupDialog = _showCreateGroupDialog.asStateFlow()
    fun toggleCreateGroupDialog() { _showCreateGroupDialog.value = !_showCreateGroupDialog.value }
    
    private val _showWalkieTalkie = MutableStateFlow(false)
    val showWalkieTalkie = _showWalkieTalkie.asStateFlow()
    fun toggleWalkieTalkie() { _showWalkieTalkie.value = !_showWalkieTalkie.value }
}
