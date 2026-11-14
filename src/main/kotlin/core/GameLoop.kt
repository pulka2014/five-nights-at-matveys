package core

class GameLoop(private val world: World, private val maxDt: Float = 0.05f) {
    private var last = System.nanoTime()
    fun tick(now: Long = System.nanoTime()) {
        val dt = ((now - last) / 1_000_000_000f).coerceAtMost(maxDt)
        last = now
        world.update(dt)
    }
    fun reset() { last = System.nanoTime() }
}
