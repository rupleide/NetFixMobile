package com.rupleide.netfix

import com.rupleide.netfix.core.help.HelpHostCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HelpHostCheckTest {

    private fun response(query: ByteArray, answers: List<ByteArray>): ByteArray {
        val header = query.copyOfRange(0, 12)
        header[2] = 0x81.toByte()
        header[3] = 0x80.toByte()
        header[7] = answers.size.toByte()
        return header + query.copyOfRange(12, query.size) + answers.fold(ByteArray(0)) { acc, it -> acc + it }
    }

    private fun answer(type: Int, rdata: ByteArray): ByteArray =
        byteArrayOf(0xC0.toByte(), 0x0C, 0, type.toByte(), 0, 1, 0, 0, 0x0E, 0x10, 0, rdata.size.toByte()) + rdata

    @Test
    fun headersEndAndContentLengthAreFound() {
        val raw = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: 51234\r\n\r\n<html>".toByteArray()
        val end = HelpHostCheck.headersEnd(raw, raw.size)
        assertEquals(raw.size - "<html>".length, end)
        assertEquals(51234L, HelpHostCheck.declaredBodyLength(String(raw, 0, end, Charsets.ISO_8859_1)))
    }

    @Test
    fun noContentLengthAndUnfinishedHeaders() {
        val chunked = "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n".toByteArray()
        assertNull(HelpHostCheck.declaredBodyLength(String(chunked, Charsets.ISO_8859_1)))
        val partial = "HTTP/1.1 200 OK\r\nContent-Length: 10".toByteArray()
        assertEquals(-1, HelpHostCheck.headersEnd(partial, partial.size))
    }

    @Test
    fun queryEncodesLabels() {
        val query = HelpHostCheck.encodeQuery("chatgpt.com", 0x1234)
        assertEquals(0x12, query[0].toInt())
        assertEquals(0x34, query[1].toInt())
        assertEquals(7, query[12].toInt())
        assertEquals("chatgpt", String(query, 13, 7))
    }

    @Test
    fun parsesFirstIpv4AfterCname() {
        val query = HelpHostCheck.encodeQuery("chatgpt.com", 77)
        val cname = answer(5, byteArrayOf(3, 'c'.code.toByte(), 'd'.code.toByte(), 'n'.code.toByte(), 0xC0.toByte(), 0x0C))
        val a = answer(1, byteArrayOf(104, 18, 32, 47))
        val data = response(query, listOf(cname, a))
        assertEquals("104.18.32.47", HelpHostCheck.parseFirstIpv4(data, data.size, 77))
    }

    @Test
    fun rejectsWrongId() {
        val query = HelpHostCheck.encodeQuery("x.com", 5)
        val data = response(query, listOf(answer(1, byteArrayOf(1, 2, 3, 4))))
        assertNull(HelpHostCheck.parseFirstIpv4(data, data.size, 6))
    }

    @Test
    fun socksUdpPacketWrapsDnsQueryForServer() {
        val query = HelpHostCheck.encodeQuery("x.com", 9)
        val packet = HelpHostCheck.socksUdpPacket("1.2.3.4", query)
        assertEquals(listOf<Byte>(0, 0, 0, 1, 1, 2, 3, 4, 0, 53), packet.take(10))
        assertEquals(10, HelpHostCheck.socksUdpPayloadOffset(packet, packet.size))
        val answer = response(query, listOf(answer(1, byteArrayOf(5, 6, 7, 8))))
        val reply = HelpHostCheck.socksUdpPacket("1.2.3.4", answer)
        val offset = HelpHostCheck.socksUdpPayloadOffset(reply, reply.size)!!
        val payload = reply.copyOfRange(offset, reply.size)
        assertEquals("5.6.7.8", HelpHostCheck.parseFirstIpv4(payload, payload.size, 9))
    }

    @Test
    fun fragmentedSocksUdpIsRejected() {
        val packet = byteArrayOf(0, 0, 1, 1, 1, 2, 3, 4, 0, 53)
        assertNull(HelpHostCheck.socksUdpPayloadOffset(packet, packet.size))
    }
}
