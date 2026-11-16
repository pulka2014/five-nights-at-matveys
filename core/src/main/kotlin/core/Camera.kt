package core

enum class Camera(val id: Int, val label: String, val short: String) {
    DINING(1, "CAM 01 - DINING", "01"),
    HALL_LEFT(2, "CAM 02 - LEFT HALL", "02"),
    HALL_RIGHT(3, "CAM 03 - RIGHT HALL", "03"),
    KITCHEN(4, "CAM 04 - KITCHEN", "04"),
    STAGE(5, "CAM 05 - STAGE", "05");

    companion object {
        fun byId(i: Int): Camera? = values().firstOrNull { it.id == i }
    }
}
