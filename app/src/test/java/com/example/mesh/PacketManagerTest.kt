package com.example.mesh

import org.junit.Assert.assertEquals
import org.junit.Test

class PacketManagerTest {

    @Test
    fun testSerializationDeserialization() {
        val originalPacket = MeshPacket(
            sourceId = "node_a",
            destId = "node_b",
            messageId = "msg_123",
            payload = "Hello Mesh!",
            packetType = PacketType.DATA,
            hopCount = 2,
            maxHops = 7
        )

        val serialized = PacketManager.serialize(originalPacket)
        val deserialized = PacketManager.deserialize(serialized)

        assertEquals(originalPacket.sourceId, deserialized.sourceId)
        assertEquals(originalPacket.destId, deserialized.destId)
        assertEquals(originalPacket.messageId, deserialized.messageId)
        assertEquals(originalPacket.payload, deserialized.payload)
        assertEquals(originalPacket.packetType, deserialized.packetType)
        assertEquals(originalPacket.hopCount, deserialized.hopCount)
    }

    @Test
    fun testChunking() {
        val largePayload = "A".repeat(1000)
        val originalPacket = MeshPacket(
            sourceId = "node_a",
            destId = "node_b",
            payload = largePayload
        )

        val chunks = PacketManager.chunkPacket(originalPacket)
        assertEquals(3, chunks.size) // 1000 / 400 = 2.5 -> 3 chunks
        assertEquals(3, chunks[0].totalChunks.toInt())
        assertEquals(0, chunks[0].chunkIdx.toInt())
        assertEquals(400, chunks[0].payload.length)
        assertEquals(200, chunks[2].payload.length)
    }
}
