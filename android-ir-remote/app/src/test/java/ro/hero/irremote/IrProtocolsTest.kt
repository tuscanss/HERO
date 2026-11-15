package ro.hero.irremote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.hero.irremote.ir.IrFormats
import ro.hero.irremote.ir.IrProtocols

class IrProtocolsTest {
    @Test fun necRoundTrip() {
        val code = IrProtocols.nec(0x00, 0x45)
        assertEquals(67, code.pattern.size)
        assertEquals(9000, code.pattern[0])
        val d = IrProtocols.decode(code.pattern)
        assertNotNull(d)
        assertEquals("NEC", d!!.protocol); assertEquals(0x00, d.address); assertEquals(0x45, d.command)
    }

    @Test fun decodeNoisyCapture() {
        // simulăm un receptor TSOP: pulsuri +80µs, pauze −80µs
        val clean = IrProtocols.nec(0xEF, 0x12).pattern
        val noisy = clean.mapIndexed { i, v -> if (i % 2 == 0) v + 80 else v - 80 } + 40000
        val code = IrProtocols.normalizeCapture(38000, noisy)
        assertEquals("NEC 0xEF/0x12", code.description)
        assertEquals(clean, code.pattern)
    }

    @Test fun necExtAndSamsung() {
        assertEquals("NECext", IrProtocols.decode(IrProtocols.necExt(0xEF00, 0xFC03).pattern)!!.protocol)
        assertEquals("Samsung32", IrProtocols.decode(IrProtocols.samsung32(0x07, 0x02).pattern)!!.protocol)
    }

    @Test fun manchesterProtocolsStartWithMark() {
        for (p in listOf("RC5", "RC5X", "RC6", "SIRC", "SIRC15", "SIRC20")) {
            val c = IrProtocols.encode(p, 0x05, 0x0C)
            assertTrue(p, c.pattern.size % 2 == 1)
            assertTrue(p, c.pattern.all { it > 0 })
        }
    }

    @Test fun prontoRoundTrip() {
        val code = IrProtocols.nec(0x01, 0x02)
        val pronto = IrFormats.toPronto(code)
        val back = IrFormats.parsePronto(pronto)
        assertTrue(kotlin.math.abs(back.frequency - 38000) < 500)
        val d = IrProtocols.decode(back.pattern)
        assertEquals(0x02, d!!.command)
    }

    @Test fun flipperParse() {
        val file = """
            Filetype: IR signals file
            Version: 1
            # 
            name: Power
            type: parsed
            protocol: NEC
            address: 07 00 00 00
            command: 02 00 00 00
            # 
            name: Warm
            type: raw
            frequency: 38000
            duty_cycle: 0.330000
            data: 9000 4500 560 560 560 1690 560
        """.trimIndent()
        val codes = IrFormats.parseFlipper(file)
        assertEquals(2, codes.size)
        assertEquals("NEC 0x07/0x02", codes[0].code.description)
        assertEquals(7, codes[1].code.pattern.size)
        val again = IrFormats.parseFlipper(IrFormats.toFlipper(codes))
        assertEquals(codes.map { it.code.pattern }, again.map { it.code.pattern })
    }
}
