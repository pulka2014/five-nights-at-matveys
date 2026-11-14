package core

enum class Camera(val id: Int, val label: String, val short: String) {
    OFFICE(1, "CAM 01 — ОФИС", "01"),
    HALL_LEFT(2, "CAM 02 — ЛЕВЫЙ КОРИДОР", "02"),
    HALL_RIGHT(3, "CAM 03 — ПРАВЫЙ КОРИДОР", "03"),
    KITCHEN(4, "CAM 04 — КУХНЯ", "04"),
    STAGE(5, "CAM 05 — СЦЕНА", "05");

    companion object {
        fun byId(i: Int): Camera? = values().firstOrNull { it.id == i }
    }
}
