package core

data class Vec2(var x: Float, var y: Float) {
    fun add(dx: Float, dy: Float) { x += dx; y += dy }
}

open class Entity(
    var pos: Vec2 = Vec2(0f, 0f),
    var w: Float = 32f,
    var h: Float = 32f,
    var spriteName: String = ""
) {
    var vx: Float = 0f
    var vy: Float = 0f

    open fun update(dt: Float) {
        pos.add(vx * dt, vy * dt)
    }

    fun overlaps(o: Entity): Boolean =
        pos.x < o.pos.x + o.w && pos.x + w > o.pos.x &&
        pos.y < o.pos.y + o.h && pos.y + h > o.pos.y
}
