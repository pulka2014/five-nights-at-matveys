package core

enum class Door { LEFT, RIGHT }

enum class MatveyState { HIDDEN, APPROACHING, AT_DOOR, RETREATING }

class NightConfig(
    val nightIndex: Int,
    val speedMultiplier: Float,
    val drainMultiplier: Float,
    val approachSeconds: Float
) {
    val name: String get() = when (nightIndex) {
        0 -> "Понедельник"
        1 -> "Вторник"
        2 -> "Среда"
        3 -> "Четверг"
        4 -> "Пятница"
        else -> "Ночь ${nightIndex + 1}"
    }
}

object NightTable {
    val nights = listOf(
        NightConfig(0, 1.0f, 1.0f, 14f),
        NightConfig(1, 1.25f, 1.15f, 12f),
        NightConfig(2, 1.55f, 1.3f, 10f),
        NightConfig(3, 1.9f, 1.5f, 8.5f),
        NightConfig(4, 2.4f, 1.75f, 7f)
    )
    fun byIndex(i: Int): NightConfig = nights[i.coerceIn(0, nights.lastIndex)]
}
