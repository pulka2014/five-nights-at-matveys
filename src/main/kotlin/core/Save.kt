package core

import java.io.File

object Save {
    private val dir = File(System.getProperty("user.home"), ".fnaf_matvey")
    private val file = File(dir, "save.json")

    data class Data(val nightIndex: Int, val finished: Boolean)

    fun load(): Data {
        if (!file.exists()) return Data(0, false)
        return runCatching {
            val t = file.readText()
            val night = Regex("\"nightIndex\"\\s*:\\s*(\\d+)").find(t)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val fin = Regex("\"finished\"\\s*:\\s*(true|false)").find(t)?.groupValues?.get(1)?.toBoolean() ?: false
            Data(night.coerceIn(0, 4), fin)
        }.getOrDefault(Data(0, false))
    }

    fun save(nightIndex: Int, finished: Boolean) {
        runCatching {
            dir.mkdirs()
            file.writeText("{\"nightIndex\":${nightIndex.coerceIn(0, 4)},\"finished\":$finished}")
        }
    }

    fun reset() { runCatching { file.delete() } }
}
