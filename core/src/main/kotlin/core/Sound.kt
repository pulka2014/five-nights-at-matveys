package core

object Sound {
    fun preload(names: List<Pair<String, Long>>) {}
    fun playOnce(name: String, volumeDb: Float = 0f) {}
    fun stop(name: String) {}
    fun stopAll() {}
}
