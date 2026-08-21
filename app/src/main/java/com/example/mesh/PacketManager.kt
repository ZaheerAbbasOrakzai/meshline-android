package com.example.mesh

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.UUID

object PacketManager {

    private const val MAX_CHUNK_SIZE = 400 // Safe limit for BLE MTU considerations

    fun serialize(packet: MeshPacket): ByteArray {
        val srcBytes = packet.sourceId.toByteArray(StandardCharsets.UTF_8)
        val destBytes = packet.destId.toByteArray(StandardCharsets.UTF_8)
        val msgIdBytes = packet.messageId.toByteArray(StandardCharsets.UTF_8)
        val payloadBytes = packet.payload.toByteArray(StandardCharsets.UTF_8)
        val hmacBytes = packet.hmac.toByteArray(StandardCharsets.UTF_8)

        // Calculate size: header fields + length prefix bytes + data bytes
        val bufferSize = 1 + 1 + 1 + 1 + 2 + 2 + 2 + 
                        1 + srcBytes.size + 
                        1 + destBytes.size + 
                        1 + msgIdBytes.size + 
                        2 + payloadBytes.size + 
                        1 + hmacBytes.size

        val buffer = ByteBuffer.allocate(bufferSize)
        buffer.put(packet.version)
        buffer.put(packet.packetType.code)
        buffer.put(packet.hopCount.toByte())
        buffer.put(packet.maxHops.toByte())
        buffer.putShort(packet.seq)
        buffer.putShort(packet.totalChunks)
        buffer.putShort(packet.chunkIdx)

        buffer.put(srcBytes.size.toByte())
        buffer.put(srcBytes)

        buffer.put(destBytes.size.toByte())
        buffer.put(destBytes)

        buffer.put(msgIdBytes.size.toByte())
        buffer.put(msgIdBytes)

        buffer.putShort(payloadBytes.size.toShort())
        buffer.put(payloadBytes)

        buffer.put(hmacBytes.size.toByte())
        buffer.put(hmacBytes)

        return buffer.array()
    }

    fun deserialize(data: ByteArray): MeshPacket {
        val buffer = ByteBuffer.wrap(data)
        val version = buffer.get()
        val typeCode = buffer.get()
        val hopCount = buffer.get().toInt()
        val maxHops = buffer.get().toInt()
        val seq = buffer.getShort()
        val totalChunks = buffer.getShort()
        val chunkIdx = buffer.getShort()

        val srcLen = buffer.get().toInt() and 0xFF
        val srcBytes = ByteArray(srcLen)
        buffer.get(srcBytes)
        val sourceId = String(srcBytes, StandardCharsets.UTF_8)

        val destLen = buffer.get().toInt() and 0xFF
        val destBytes = ByteArray(destLen)
        buffer.get(destBytes)
        val destId = String(destBytes, StandardCharsets.UTF_8)

        val msgIdLen = buffer.get().toInt() and 0xFF
        val msgIdBytes = ByteArray(msgIdLen)
        buffer.get(msgIdBytes)
        val messageId = String(msgIdBytes, StandardCharsets.UTF_8)

        val payloadLen = buffer.getShort().toInt() and 0xFFFF
        val payloadBytes = ByteArray(payloadLen)
        buffer.get(payloadBytes)
        val payload = String(payloadBytes, StandardCharsets.UTF_8)

        val hmacLen = buffer.get().toInt() and 0xFF
        val hmacBytes = ByteArray(hmacLen)
        buffer.get(hmacBytes)
        val hmac = String(hmacBytes, StandardCharsets.UTF_8)

        return MeshPacket(
            version = version,
            sourceId = sourceId,
            destId = destId,
            messageId = messageId,
            hopCount = hopCount,
            maxHops = maxHops,
            packetType = PacketType.fromCode(typeCode),
            seq = seq,
            totalChunks = totalChunks,
            chunkIdx = chunkIdx,
            payload = payload,
            hmac = hmac
        )
    }

    /**
     * Chunks a large packet into smaller packets if the payload exceeds MAX_CHUNK_SIZE.
     */
    fun chunkPacket(packet: MeshPacket): List<MeshPacket> {
        val payloadBytes = packet.payload.toByteArray(StandardCharsets.UTF_8)
        if (payloadBytes.size <= MAX_CHUNK_SIZE) return listOf(packet)

        val chunks = mutableListOf<MeshPacket>()
        val totalChunks = Math.ceil(payloadBytes.size.toDouble() / MAX_CHUNK_SIZE).toInt()

        for (i in 0 until totalChunks) {
            val start = i * MAX_CHUNK_SIZE
            val end = Math.min(start + MAX_CHUNK_SIZE, payloadBytes.size)
            val chunkPayload = String(payloadBytes.copyOfRange(start, end), StandardCharsets.UTF_8)

            chunks.add(
                packet.copy(
                    totalChunks = totalChunks.toShort(),
                    chunkIdx = i.toShort(),
                    payload = chunkPayload
                )
            )
        }
        return chunks
    }

    private val chunkBuffer = mutableMapOf<String, MutableMap<Short, String>>()

    /**
     * Reassembles chunks into a single payload. Returns the payload string when all chunks are present, else null.
     */
    fun handleChunk(packet: MeshPacket): String? {
        if (packet.totalChunks <= 1) return packet.payload

        val messageChunks = chunkBuffer.getOrPut(packet.messageId) { mutableMapOf() }
        messageChunks[packet.chunkIdx] = packet.payload

        if (messageChunks.size == packet.totalChunks.toInt()) {
            val fullPayload = StringBuilder()
            for (i in 0 until packet.totalChunks) {
                fullPayload.append(messageChunks[i.toShort()] ?: "")
            }
            chunkBuffer.remove(packet.messageId)
            return fullPayload.toString()
        }

        return null
    }
}
