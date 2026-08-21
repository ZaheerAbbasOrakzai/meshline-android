package com.example.mesh

import java.util.UUID

enum class PacketType(val code: Byte) {
    DISCOVERY(0x01.toByte()),
    ROUTE_REQUEST(0x02.toByte()),
    ROUTE_REPLY(0x03.toByte()),
    DATA(0x04.toByte()),
    ACK(0x05.toByte()),
    KEY_EXCHANGE(0x06.toByte()),
    GROUP_INVITE(0x07.toByte()),
    RELAY(0x08.toByte()),
    HEARTBEAT(0x09.toByte()),
    KEY_ROTATION(0x0A.toByte());

    companion object {
        fun fromCode(code: Byte): PacketType {
            return entries.firstOrNull { it.code == code } ?: DATA
        }
    }
}

data class MeshPacket(
    val version: Byte = 0x01,
    val sourceId: String,
    val destId: String,
    val messageId: String = UUID.randomUUID().toString(),
    var hopCount: Int = 1,
    val maxHops: Int = 7,
    val packetType: PacketType = PacketType.DATA,
    val seq: Short = 1,
    val totalChunks: Short = 1,
    val chunkIdx: Short = 0,
    val payload: String,
    val hmac: String = ""
) {
    fun toFrameSummary(): String {
        return "v$version | ${packetType.name} | Hop $hopCount/$maxHops | Src: ${sourceId.take(8)}... -> Dst: ${destId.take(8)}..."
    }
}
