package core

import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.math.pow

object Sound {
    private val dir: File by lazy { resolveDir() }
    private class Sample(val bytes: ByteArray, val fmt: AudioFormat)
    private val cache = mutableMapOf<String, Sample>()
    private val loops = mutableMapOf<String, Thread>()

    private fun resolveDir(): File {
        val a = File("assets/sounds"); if (a.exists()) return a
        val loc = runCatching { File(Sound::class.java.protectionDomain.codeSource.location.toURI()) }.getOrNull()
        if (loc != null) {
            var d = if (loc.isDirectory) loc else loc.parentFile
            repeat(4) { val c = File(d, "assets/sounds"); if (c.exists()) return c; d = d?.parentFile ?: return@repeat }
        }
        val h = File(System.getProperty("user.home"), "game/assets/sounds"); if (h.exists()) return h
        System.err.println("[Sound] assets/sounds not found; cwd=" + File(".").absolutePath); return a
    }

    fun preload(names: List<Pair<String, Long>>) {
        if (!dir.exists()) { System.err.println("[Sound] dir missing: " + dir.absolutePath); return }
        for ((n, clipMs) in names) {
            val f = File(dir, n)
            if (!f.exists()) { System.err.println("[Sound] no file: " + f.absolutePath); continue }
            runCatching {
                val ais: AudioInputStream = AudioSystem.getAudioInputStream(f)
                val fmt = ais.format
                var raw = ais.readBytes()
                if (clipMs > 0) {
                    val bytes = ((clipMs * fmt.sampleRate / 1000) * fmt.frameSize).toInt()
                    if (raw.size > bytes) raw = raw.copyOf(bytes)
                }
                cache[n] = Sample(raw, fmt)
                System.err.println("[Sound] loaded: $n (${raw.size / fmt.frameSize} frames, ${raw.size * 1000L / (fmt.sampleRate.toLong() * fmt.frameSize)} ms)")
            }.onFailure { System.err.println("[Sound] load FAILED: $n → ${it::class.simpleName}: ${it.message}") }
        }
    }

    private fun dbToFactor(db: Float): Double = 10.0.pow(db / 20.0)

    private fun scaled(bytes: ByteArray, factor: Double): ByteArray {
        val out = ByteArray(bytes.size)
        var i = 0
        while (i + 1 < bytes.size) {
            val lo = bytes[i].toInt() and 0xff
            val hi = bytes[i + 1].toInt()
            val v = (hi shl 8) or lo
            val sv = if (v > 32767) v - 65536 else v
            val nv = (sv * factor).toInt().coerceIn(-32768, 32767)
            out[i] = (nv and 0xff).toByte()
            out[i + 1] = ((nv shr 8) and 0xff).toByte()
            i += 2
        }
        return out
    }

    fun playOnce(name: String, volumeDb: Float = 0f) {
        val s = cache[name] ?: run { System.err.println("[Sound] playOnce: no $name"); return }
        val fmt = s.fmt
        val info = DataLine.Info(SourceDataLine::class.java, fmt)
        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("[Sound] line not supported for $name: $fmt"); return
        }
        stop(name)
        val data = if (volumeDb == 0f) s.bytes else scaled(s.bytes, dbToFactor(volumeDb))
        val t = Thread {
            runCatching {
                val line = AudioSystem.getLine(info) as SourceDataLine
                line.open(fmt); line.start()
                runCatching {
                    (line.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl)?.value = volumeDb.coerceIn(-80f, 6f)
                }
                line.write(data, 0, data.size)
                line.drain(); line.stop(); line.close()
            }.onFailure { System.err.println("[Sound] playOnce FAILED: $name → ${it.message}") }
        }.apply { isDaemon = true; this.name = "snd-$name"; start() }
        loops[name] = t
    }

    fun play(name: String, volumeDb: Float = 0f, loop: Boolean = false) {
        if (!loop) { playOnce(name, volumeDb); return }
        val s = cache[name] ?: run { System.err.println("[Sound] play: no $name"); return }
        val fmt = s.fmt
        val info = DataLine.Info(SourceDataLine::class.java, fmt)
        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("[Sound] line not supported for $name: $fmt"); return
        }
        stop(name)
        val data = if (volumeDb == 0f) s.bytes else scaled(s.bytes, dbToFactor(volumeDb))
        val t = Thread {
            runCatching {
                val line = AudioSystem.getLine(info) as SourceDataLine
                line.open(fmt); line.start()
                runCatching {
                    (line.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl)?.value = volumeDb.coerceIn(-80f, 6f)
                }
                while (!Thread.currentThread().isInterrupted) line.write(data, 0, data.size)
                line.drain(); line.stop(); line.close()
            }.onFailure { System.err.println("[Sound] loop FAILED: $name → ${it.message}") }
        }.apply { isDaemon = true; this.name = "snd-$name"; start() }
        loops[name] = t
    }

    fun stop(name: String) { loops.remove(name)?.interrupt() }
    fun stopAll() { loops.keys.toList().forEach { stop(it) } }
}
