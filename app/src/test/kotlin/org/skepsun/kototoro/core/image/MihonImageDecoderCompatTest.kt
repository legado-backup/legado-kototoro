package org.skepsun.kototoro.core.image

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MihonImageDecoderCompatTest {

    @Test
    fun `recognizes jpeg xl codestream signature`() {
        assertTrue(MihonImageDecoderCompat.isJxl(byteArrayOf(0xFF.toByte(), 0x0A, 0x01)))
    }

    @Test
    fun `recognizes jpeg xl container signature`() {
        assertTrue(
            MihonImageDecoderCompat.isJxl(
                byteArrayOf(
                    0x00,
                    0x00,
                    0x00,
                    0x0C,
                    0x4A,
                    0x58,
                    0x4C,
                    0x20,
                    0x0D,
                    0x0A,
                    0x87.toByte(),
                    0x0A,
                ),
            ),
        )
    }

    @Test
    fun `does not classify jpeg as jpeg xl`() {
        assertFalse(MihonImageDecoderCompat.isJxl(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())))
    }
}
