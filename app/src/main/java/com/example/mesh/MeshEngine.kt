package com.example.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MeshEngine(private val context: Context) {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        val DATA_CHAR_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567891")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _peers = MutableStateFlow<List<MeshPeer>>(emptyList())
    val peers: StateFlow<List<MeshPeer>> = _peers.asStateFlow()

    private val _discoveredNearbyNodes = MutableStateFlow<List<MeshPeer>>(emptyList())
    val discoveredNearbyNodes: StateFlow<List<MeshPeer>> = _discoveredNearbyNodes.asStateFlow()

    private val _routes = MutableStateFlow<List<MeshRoute>>(emptyList())
    val routes: StateFlow<List<MeshRoute>> = _routes.asStateFlow()

    private val _packetLogs = MutableStateFlow<List<PacketLogEntry>>(emptyList())
    val packetLogs: StateFlow<List<PacketLogEntry>> = _packetLogs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isRelayEnabled = MutableStateFlow(true)
    val isRelayEnabled: StateFlow<Boolean> = _isRelayEnabled.asStateFlow()

    private val seenMessageIds = mutableMapOf<String, Long>()
    
    private fun addToSeenPackets(msgId: String) {
        val now = System.currentTimeMillis()
        if (seenMessageIds.size > 500) {
            val iterator = seenMessageIds.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value > 600000) {
                    iterator.remove()
                }
            }
        }
        seenMessageIds[msgId] = now
    }

    private var meshService: MeshService? = null
    private var onPacketReceivedCallback: ((MeshPacket) -> Unit)? = null
    private var localNodeId: String = "unassigned"

    fun setService(service: MeshService) {
        this.meshService = service
    }

    fun setLocalNodeId(id: String) {
        this.localNodeId = id
    }

    fun setOnPacketReceivedCallback(callback: (MeshPacket) -> Unit) {
        this.onPacketReceivedCallback = callback
    }

    fun onNeighborConnected(address: String) {
        logCustomPacket(
            sourceName = "System",
            destName = "Local Node",
            type = PacketType.HEARTBEAT,
            hopCount = 0,
            maxHops = 0,
            status = "GATT connection established with neighbor: $address"
        )
    }

    fun onPacketReceived(packet: MeshPacket, neighborAddress: String) {
        if (seenMessageIds.containsKey(packet.messageId) && packet.totalChunks <= 1) return
        
        val fullPayload = PacketManager.handleChunk(packet) ?: return

        val fullPacket = packet.copy(payload = fullPayload)
        addToSeenPackets(fullPacket.messageId)

        if (fullPacket.hopCount > fullPacket.maxHops) {
            logPacket(PacketLogEntry(
                sourceName = fullPacket.sourceId,
                destName = fullPacket.destId,
                packetType = fullPacket.packetType,
                hopCount = fullPacket.hopCount,
                maxHops = fullPacket.maxHops,
                status = "TTL Expired"
            ))
            return
        }

        val isForMe = fullPacket.destId == localNodeId || fullPacket.destId == "broadcast_all"
        if (isForMe) {
            logCustomPacket(fullPacket.sourceId, "Me", fullPacket.packetType, fullPacket.hopCount, fullPacket.maxHops, "Received mesh packet")
            onPacketReceivedCallback?.invoke(fullPacket)
        }

        if (_isRelayEnabled.value && (fullPacket.destId != localNodeId)) {
            val relayPacket = fullPacket.copy(hopCount = fullPacket.hopCount + 1)
            if (relayPacket.hopCount <= relayPacket.maxHops) {
                logCustomPacket(relayPacket.sourceId, relayPacket.destId, relayPacket.packetType, relayPacket.hopCount, relayPacket.maxHops, "Relaying packet...")
                broadcastPacket(relayPacket)
            }
        }
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startBleDiscovery() {
        _isScanning.value = true
        logCustomPacket("Local Node", "BLE Broadcast", PacketType.DISCOVERY, 0, 1, "Scanning for mesh GATT beacons...")
    }

    fun stopBleDiscovery() {
        _isScanning.value = false
    }

    fun registerConnectedPeer(peer: MeshPeer) {
        val updated = (_peers.value.filter { it.id != peer.id } + peer)
        _peers.value = updated
        _discoveredNearbyNodes.value = _discoveredNearbyNodes.value.filter { it.nodeId != peer.nodeId }
        updateRoutingTable(updated)
        logCustomPacket(peer.name, "Local Node", PacketType.KEY_EXCHANGE, peer.hops, 7, "Node joined mesh link")
    }

    fun syncPeersWithContacts(contacts: List<com.example.data.model.ContactEntity>) {
        val activePeers = contacts.filter { it.id != "saved_messages" && it.id != "public_channel" }.map { c ->
            MeshPeer(
                id = c.id,
                name = c.name,
                nodeId = c.nodeId,
                rssi = if (c.online) -55 else -92,
                hops = if (c.lastSeen.contains("2 Hop")) 2 else if (c.lastSeen.contains("3 Hop")) 3 else 1,
                state = if (c.online) PeerState.CONNECTED else PeerState.OFFLINE,
                isRelayNode = c.name.contains("Relay", ignoreCase = true)
            )
        }
        _peers.value = activePeers
        updateRoutingTable(activePeers)
    }

    fun addDiscoveredNode(peer: MeshPeer) {
        if (_peers.value.none { it.nodeId == peer.nodeId }) {
            val current = _discoveredNearbyNodes.value.filter { it.nodeId != peer.nodeId }
            _discoveredNearbyNodes.value = current + peer
        }
    }

    fun removePeer(peerId: String) {
        val updated = _peers.value.filter { it.id != peerId }
        _peers.value = updated
        updateRoutingTable(updated)
    }

    fun toggleRelayMode() {
        _isRelayEnabled.value = !_isRelayEnabled.value
        logCustomPacket("System", "Local Node", PacketType.HEARTBEAT, 0, 0, "Relay mode: ${if (_isRelayEnabled.value) "Active" else "Disabled"}")
    }

    fun routePacket(packet: MeshPacket, senderName: String, recipientName: String): PacketLogEntry {
        if (seenMessageIds.containsKey(packet.messageId)) {
            val entry = PacketLogEntry(
                sourceName = senderName,
                destName = recipientName,
                packetType = packet.packetType,
                hopCount = packet.hopCount,
                maxHops = packet.maxHops,
                status = "Dropped duplicate"
            )
            logPacket(entry)
            return entry
        }

        addToSeenPackets(packet.messageId)

        if (packet.hopCount > packet.maxHops) {
            val entry = PacketLogEntry(
                sourceName = senderName,
                destName = recipientName,
                packetType = packet.packetType,
                hopCount = packet.hopCount,
                maxHops = packet.maxHops,
                status = "TTL Exceeded"
            )
            logPacket(entry)
            return entry
        }

        val statusMsg = if (packet.hopCount == 1) "Delivered via direct BLE" else "Forwarded through ${packet.hopCount - 1} relay(s)"
        val entry = PacketLogEntry(
            sourceName = senderName,
            destName = recipientName,
            packetType = packet.packetType,
            hopCount = packet.hopCount,
            maxHops = packet.maxHops,
            status = statusMsg,
            isForwarded = packet.hopCount > 1
        )
        logPacket(entry)

        val chunks = PacketManager.chunkPacket(packet)
        chunks.forEach { broadcastPacket(it) }

        return entry
    }

    fun broadcastPacket(packet: MeshPacket) {
        meshService?.sendPacketToAllNeighbors(packet)
    }

    fun logCustomPacket(sourceName: String, destName: String, type: PacketType, hopCount: Int, maxHops: Int, status: String) {
        logPacket(PacketLogEntry(
            sourceName = sourceName,
            destName = destName,
            packetType = type,
            hopCount = hopCount,
            maxHops = maxHops,
            status = status
        ))
    }

    private fun logPacket(entry: PacketLogEntry) {
        _packetLogs.value = (listOf(entry) + _packetLogs.value).take(50)
    }

    private fun updateRoutingTable(peerList: List<MeshPeer>) {
        _routes.value = peerList.map { peer ->
            MeshRoute(
                destinationId = peer.nodeId,
                nextHopNodeId = if (peer.hops == 1) peer.nodeId else (peerList.firstOrNull { it.isRelayNode }?.nodeId ?: peer.nodeId),
                hopCount = peer.hops,
                qualityScore = (1.0f / peer.hops).coerceAtLeast(0.3f)
            )
        }
    }

    fun clearAll() {
        _peers.value = emptyList()
        _discoveredNearbyNodes.value = emptyList()
        _routes.value = emptyList()
        _packetLogs.value = emptyList()
        seenMessageIds.clear()
    }
}
