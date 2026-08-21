package com.example.mesh

enum class PeerState {
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    OFFLINE
}

data class MeshPeer(
    val id: String,
    val name: String,
    val nodeId: String,
    val rssi: Int = -70,
    val hops: Int = 1,
    val state: PeerState = PeerState.DISCOVERING,
    val isRelayNode: Boolean = false
) {
    val signalPercent: Int
        get() = ((rssi + 100) * 2).coerceIn(0, 100)
}

data class MeshRoute(
    val destinationId: String,
    val nextHopNodeId: String,
    val hopCount: Int,
    val qualityScore: Float = 1.0f
)

data class PacketLogEntry(
    val sourceName: String,
    val destName: String,
    val packetType: PacketType,
    val hopCount: Int,
    val maxHops: Int,
    val status: String,
    val isForwarded: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
