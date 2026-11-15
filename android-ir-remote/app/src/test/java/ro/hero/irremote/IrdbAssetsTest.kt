package ro.hero.irremote

import org.junit.Assert.assertTrue
import org.junit.Test
import ro.hero.irremote.data.CodeSet
import ro.hero.irremote.ir.IrFormats
import java.io.File

class IrdbAssetsTest {
    @Test fun allBundledFilesParse() {
        val root = File("src/main/assets/irdb")
        val files = root.walk().filter { it.extension == "ir" }.toList()
        assertTrue("fișiere: ${files.size}", files.size >= 150)
        var parsed = 0
        var ceiling = 0
        var withProbe = 0
        val failed = mutableListOf<String>()
        for (f in files) {
            val codes = IrFormats.parseFlipper(f.readText())
            if (codes.isEmpty()) { failed += f.name; continue }
            parsed++
            codes.forEach { assertTrue(f.name + " " + it.name, it.code.isValid()) }
            val set = CodeSet("b", f.name, f.path, codes, CodeSet.isLikelyCeiling(f.path, codes))
            if (set.likelyCeiling) ceiling++
            if (set.probeButton != null) withProbe++
        }
        println("parsed=$parsed/${files.size} ceiling=$ceiling withProbe=$withProbe failed=$failed")
        assertTrue(parsed >= files.size * 0.9)
    }
}
