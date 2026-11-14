package desktop

import core.Door
import core.GameLoop
import core.MatveyState
import core.NightTable
import core.Save
import core.World
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

class AppFrame : JFrame("Five Nights at Matvey's") {
    private val menu = MenuPanel(
        onNewGame = { showGame(0, fresh = true) },
        onContinue = { night -> showGame(night, fresh = false) }
    )
    private var game: GamePanel? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(960, 640)
        setLocationRelativeTo(null)
        contentPane = menu
        isVisible = true
        menu.requestFocusInWindow()

        // при закрытии окна — сохранить текущую ночь
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent) {
                game?.let { g -> g.stop() }
            }
        })
    }

    fun showMenu() {
        game?.let { it.stop() }
        game = null
        contentPane = menu
        menu.refresh()
        menu.requestFocusInWindow()
        revalidate(); repaint()
    }

    fun showGame(night: Int, fresh: Boolean) {
        val g = GamePanel(
            startNight = night,
            freshStart = fresh,
            onExitToMenu = { showMenu() },
            onSave = { idx, fin -> Save.save(idx, fin) }
        )
        game = g
        contentPane = g
        g.requestFocusInWindow()
        revalidate(); repaint()
    }
}

class GamePanel(
    startNight: Int,
    freshStart: Boolean,
    private val onExitToMenu: () -> Unit,
    private val onSave: (Int, Boolean) -> Unit
) : JPanel() {
    private var world = World()
    private var loop = GameLoop(world)
    private var nightIndex = startNight
    private val sprites = mutableMapOf<String, BufferedImage>()
    private var timer: Timer? = null
    private var autoSaveTimer: Timer? = null
    @Volatile private var running = true
    private var camMode = false

    private fun toggleCamMode() { camMode = !camMode; repaint() }

    init {
        background = Color(12, 12, 16)
        isFocusable = true
        loadSprites()
        if (freshStart) {
            Save.reset()
            onSave(0, false)
        }
        world.newNightHardReset(nightIndex)
        onSave(nightIndex, false)

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_A, KeyEvent.VK_LEFT -> world.toggleDoor(Door.LEFT)
                    KeyEvent.VK_D, KeyEvent.VK_RIGHT -> world.toggleDoor(Door.RIGHT)
                    KeyEvent.VK_R -> restartNight(nightIndex)
                    KeyEvent.VK_N -> if (world.won && nightIndex < 4) restartNight(nightIndex + 1)
                    KeyEvent.VK_ESCAPE -> onExitToMenu()
                    KeyEvent.VK_TAB, KeyEvent.VK_C -> toggleCamMode()
                    KeyEvent.VK_1 -> world.switchCamera(core.Camera.OFFICE)
                    KeyEvent.VK_2 -> world.switchCamera(core.Camera.HALL_LEFT)
                    KeyEvent.VK_3 -> world.switchCamera(core.Camera.HALL_RIGHT)
                    KeyEvent.VK_4 -> world.switchCamera(core.Camera.KITCHEN)
                    KeyEvent.VK_5 -> world.switchCamera(core.Camera.STAGE)
                }
            }
        })
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (world.gameOver) { restartNight(nightIndex); return }
                if (world.won) { if (nightIndex < 4) restartNight(nightIndex + 1); return }
                if (leftButtonRect().contains(e.point)) world.toggleDoor(Door.LEFT)
                if (rightButtonRect().contains(e.point)) world.toggleDoor(Door.RIGHT)
                if (camMode) {
                    val pad = 16; val mapW = 360; val mapH = 200
                    val x0 = width - mapW - pad; val y0 = height - mapH - pad
                    val roomsPx = listOf(
                        core.Camera.STAGE      to (x0 + mapW / 2 to y0 + 40),
                        core.Camera.HALL_LEFT  to (x0 + 70 to y0 + 100),
                        core.Camera.KITCHEN    to (x0 + mapW / 2 to y0 + 100),
                        core.Camera.HALL_RIGHT to (x0 + mapW - 70 to y0 + 100),
                        core.Camera.OFFICE     to (x0 + mapW / 2 to y0 + mapH - 40)
                    )
                    for ((cam, pos) in roomsPx) {
                        val dx = e.x - pos.first; val dy = e.y - pos.second
                        if (dx * dx + dy * dy <= 24 * 24) { world.switchCamera(cam); repaint(); return }
                    }
                }
            }
        })
        timer = Timer(16) { if (running) { loop.tick(); repaint() } }.also { it.start() }
        // автосохранение каждые 5 секунд (текущая ночь + факт победы)
        autoSaveTimer = Timer(5000) {
            if (running) onSave(nightIndex, world.won && nightIndex >= 4)
        }.also { it.start() }
    }

    fun stop() { running = false; timer?.stop(); autoSaveTimer?.stop(); onSave(nightIndex, world.won && nightIndex >= 4) }

    private fun loadSprites() {
        val dir = File("assets/sprites")
        if (dir.exists()) {
            dir.listFiles { f -> f.extension.lowercase() in setOf("png", "jpg") }
                ?.forEach { f -> runCatching { sprites[f.name] = ImageIO.read(f) } }
        }
    }

    private fun restartNight(i: Int) {
        nightIndex = i
        world = World().also { it.newNightHardReset(i) }
        loop = GameLoop(world)
        onSave(i, false)
    }

    private fun leftButtonRect() = Rectangle(20, height / 2 - 130, 140, 260)
    private fun rightButtonRect() = Rectangle(width - 160, height / 2 - 130, 140, 260)

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)

        if (camMode) {
            drawCameraView(g2)
            drawCamMiniMap(g2)
            drawHud(g2)
            drawOverlays(g2)
        } else {
            drawOffice(g2)
            drawDoorButtons(g2)
            drawMatvey(g2)
            drawHud(g2)
            drawOverlays(g2)
        }

        if (world.won) onSave(minOf(nightIndex + 1, 4), nightIndex >= 4)
    }

    private fun drawOffice(g2: Graphics2D) {
        val w = width; val h = height
        val bg = sprites["office.png"]
        if (bg != null) {
            g2.drawImage(bg, 0, 0, w, h, null)
        } else {
            g2.color = Color(28, 30, 40); g2.fillRect(0, 0, w, h)
            g2.color = Color(20, 22, 30); g2.fillRect(160, 60, w - 320, h - 180)
        }
        g2.color = Color(45, 35, 28); g2.fillRect(w / 2 - 220, h - 180, 440, 120)
        g2.color = Color(10, 60, 60); g2.fillRect(w / 2 - 120, h - 260, 240, 80)
        g2.color = Color(30, 200, 200); g2.drawRect(w / 2 - 120, h - 260, 240, 80)
        g2.font = Font("Monospaced", Font.PLAIN, 14)
        g2.drawString("CAM 01 — OFFICE", w / 2 - 100, h - 215)
    }

    private fun drawDoorButtons(g2: Graphics2D) {
        drawOneDoor(g2, leftButtonRect(), world.leftDoorClosed, "L", "[A]/[←]")
        drawOneDoor(g2, rightButtonRect(), world.rightDoorClosed, "R", "[D]/[→]")
    }

    private fun drawOneDoor(g2: Graphics2D, rect: Rectangle, closed: Boolean, tag: String, hint: String) {
        g2.color = if (closed) Color(180, 40, 40) else Color(40, 120, 60)
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 20, 20)
        g2.color = Color.WHITE
        g2.stroke = BasicStroke(3f)
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 20, 20)
        g2.font = Font("SansSerif", Font.BOLD, 18)
        g2.drawString(if (closed) "$tag: ЗАКРЫТО" else "$tag: ОТКРЫТО", rect.x + 12, rect.y + rect.height / 2)
        g2.font = Font("SansSerif", Font.PLAIN, 11)
        g2.drawString(hint, rect.x + rect.width / 2 - 24, rect.y - 8)
    }

    private fun drawMatvey(g2: Graphics2D) {
        val visible = world.matveyState == MatveyState.AT_DOOR || world.matveyState == MatveyState.RETREATING
        if (!visible) return
        val doorClosed = when (world.matveyDoor) {
            Door.LEFT -> world.leftDoorClosed
            Door.RIGHT -> world.rightDoorClosed
        }
        val alpha = when {
            world.matveyState == MatveyState.AT_DOOR && doorClosed -> 0.35f
            world.matveyState == MatveyState.RETREATING -> 0.55f
            else -> 1f
        }

        val w = 140; val h = 180
        val off = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val og = off.createGraphics()
        og.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        og.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        val img = sprites["matvey.png"]
        if (img != null) {
            og.drawImage(img, 0, 0, w, h, null)
        } else {
            og.color = if (world.matveyState == MatveyState.AT_DOOR) Color(220, 40, 40) else Color(120, 60, 60)
            og.fillRoundRect(0, 0, w, h, 25, 25)
            og.color = Color.BLACK
            og.fillOval(40, 40, 20, 20)
            og.fillOval(80, 40, 20, 20)
        }
        og.dispose()

        // отзеркаливание для правой двери
        val mirror = world.matveyDoor == Door.RIGHT
        val x = if (!mirror) 70 else width - 210
        val y = height / 2 - 90

        val old = g2.composite
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
        if (mirror) {
            // drawImage с отражением по X: (dx1,dy1,dx2,dy2, sx1,sy1,sx2,sy2)
            g2.drawImage(off,
                x + w, y, x, y + h,       // куда (отражённо)
                0, 0, w, h,                // откуда
                null)
        } else {
            g2.drawImage(off, x, y, null)
        }
        g2.composite = old

        g2.color = Color.WHITE
        g2.font = Font("SansSerif", Font.BOLD, 12)
        g2.drawString("МАТВЕЙ У ДВЕРИ", x + 6, y - 10)
    }


    private fun drawCameraView(g2: Graphics2D) {
        val w = width; val h = height
        // фон сцены
        g2.color = Color(8, 10, 14)
        g2.fillRect(0, 0, w, h)

        // шум/текстура камеры
        val rnd = java.util.Random(world.activeCamera.id * 31L + (System.currentTimeMillis() / 120))
        for (i in 0 until 1200) {
            val x = rnd.nextInt(w); val y = rnd.nextInt(h)
            g2.color = Color(60 + rnd.nextInt(60), 60 + rnd.nextInt(60), 60 + rnd.nextInt(60), 40)
            g2.fillRect(x, y, 2, 2)
        }

        // сетка камеры (декор)
        g2.color = Color(20, 30, 25, 120)
        g2.stroke = BasicStroke(1f)
        for (x in 0 until w step 60) g2.drawLine(x, 0, x, h)
        for (y in 0 until h step 60) g2.drawLine(0, y, w, y)

        // подпись камеры + направление обзора
        val dir = when (world.activeCamera) {
            core.Camera.OFFICE -> "↑ ВХОД"
            core.Camera.HALL_LEFT -> "→ К ОФИСУ"
            core.Camera.HALL_RIGHT -> "← К ОФИСУ"
            core.Camera.KITCHEN -> "↓ ВХОД В КУХНЮ"
            core.Camera.STAGE -> "↓ СЦЕНА"
        }
        g2.color = Color(220, 240, 220)
        g2.font = Font("Monospaced", Font.BOLD, 22)
        g2.drawString(world.activeCamera.label, 20, 32)
        g2.color = Color(150, 200, 170)
        g2.font = Font("Monospaced", Font.PLAIN, 12)
        g2.drawString(dir, 20, 52)
        g2.color = Color(120, 220, 120)
        g2.fillOval(w - 40, 18, 12, 12)
        g2.font = Font("Monospaced", Font.PLAIN, 12)
        g2.drawString("REC", w - 34, 50)

        // что видно на камере
        when (world.activeCamera) {
            core.Camera.OFFICE -> drawCamOffice(g2)
            core.Camera.HALL_LEFT, core.Camera.HALL_RIGHT -> drawCamHall(g2, world.activeCamera == core.Camera.HALL_LEFT)
            core.Camera.KITCHEN -> drawCamKitchen(g2)
            core.Camera.STAGE -> drawCamStage(g2)
        }

        // Матвей на камере
        drawCamMatvey(g2)
    }

    private fun drawCamOffice(g2: Graphics2D) {
        val w = width; val h = height
        g2.color = Color(30, 32, 40); g2.fillRect(w / 2 - 300, h / 2 - 160, 600, 320)
        g2.color = Color(20, 60, 60); g2.fillRect(w / 2 - 140, h / 2 + 60, 280, 80)
        g2.color = Color(45, 35, 28); g2.fillRect(w / 2 - 220, h / 2 + 120, 440, 100)
        g2.color = Color(220, 220, 220); g2.font = Font("SansSerif", Font.PLAIN, 14)
        g2.drawString("(пусто — тебя здесь нет, ты смотришь камеры)", w / 2 - 200, h / 2 + 200)
    }

    private fun drawCamHall(g2: Graphics2D, left: Boolean) {
        val w = width; val h = height
        g2.color = Color(22, 24, 30); g2.fillRect(0, 0, w, h)
        g2.color = Color(40, 42, 50)
        // перспективный коридор
        for (i in 0..5) {
            val t = i / 5f
            val x1 = (w * (0.15 + t * 0.35)).toInt()
            val x2 = (w * (0.85 - t * 0.35)).toInt()
            val y1 = (h * (0.15 + t * 0.35)).toInt()
            val y2 = (h * (0.85 - t * 0.05)).toInt()
            g2.drawRect(x1, y1, x2 - x1, y2 - y1)
        }
        g2.color = Color(200, 180, 100); g2.fillOval(w / 2 - 30, 60, 60, 20)
        g2.color = Color(220, 220, 220); g2.font = Font("SansSerif", Font.PLAIN, 14)
        g2.drawString(if (left) "Левый коридор (дверь слева)" else "Правый коридор (дверь справа)", 20, h - 20)
    }

    private fun drawCamKitchen(g2: Graphics2D) {
        val w = width; val h = height
        g2.color = Color(18, 20, 24); g2.fillRect(0, 0, w, h)
        g2.color = Color(50, 60, 70); g2.fillRect(80, 120, w - 160, h - 240)
        g2.color = Color(90, 100, 110)
        for (i in 0..3) g2.drawRect(120 + i * 180, 160, 140, 80)
        g2.color = Color(160, 160, 170); g2.font = Font("SansSerif", Font.PLAIN, 14)
        g2.drawString("Кухня — тихо, но кто-то мог пройти", 20, h - 20)
    }

    private fun drawCamStage(g2: Graphics2D) {
        val w = width; val h = height
        g2.color = Color(16, 14, 20); g2.fillRect(0, 0, w, h)
        // сцена
        g2.color = Color(80, 50, 30); g2.fillRect(w / 2 - 320, h / 2 - 80, 640, 160)
        g2.color = Color(120, 60, 40); g2.fillRect(w / 2 - 320, h / 2 - 80, 640, 20)
        // шторы
        g2.color = Color(90, 20, 30)
        g2.fillRect(w / 2 - 340, h / 2 - 140, 40, 260)
        g2.fillRect(w / 2 + 300, h / 2 - 140, 40, 260)
        g2.color = Color(200, 200, 200); g2.font = Font("SansSerif", Font.PLAIN, 14)
        g2.drawString("Сцена — здесь Матвей ждёт своего часа", 20, h - 20)
    }

    private fun drawCamMatvey(g2: Graphics2D) {
        val onCam = world.matveyOnCamera() ?: return
        if (onCam != world.activeCamera) return
        // прозрачность: если AT_DOOR и дверь закрыта — полупрозрачный
        val doorClosed = when (world.matveyDoor) {
            Door.LEFT -> world.leftDoorClosed
            Door.RIGHT -> world.rightDoorClosed
        }
        val alpha = when {
            world.matveyState == MatveyState.AT_DOOR && doorClosed -> 0.35f
            world.matveyState == MatveyState.RETREATING -> 0.55f
            else -> 1f
        }
        val w = 140; val h = 180
        val off = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val og = off.createGraphics()
        og.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        val img = sprites["matvey.png"]
        if (img != null) {
            og.drawImage(img, 0, 0, w, h, null)
        } else {
            og.color = Color(220, 40, 40); og.fillRoundRect(0, 0, w, h, 25, 25)
            og.color = Color.BLACK; og.fillOval(40, 40, 20, 20); og.fillOval(80, 40, 20, 20)
        }
        og.dispose()
        val mirror = world.matveyDoor == Door.RIGHT
        val x = width / 2 - w / 2
        val y = height / 2 - h / 2
        val old = g2.composite
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
        if (mirror) g2.drawImage(off, x + w, y, x, y + h, 0, 0, w, h, null)
        else g2.drawImage(off, x, y, null)
        g2.composite = old
        g2.color = Color(255, 200, 200); g2.font = Font("Monospaced", Font.BOLD, 14)
        g2.drawString("МАТВЕЙ", x, y - 6)
    }

    private fun drawCamMiniMap(g2: Graphics2D) {
        // «Карта объекта»: схема комнат + точки камер с направлением обзора
        val pad = 16
        val mapW = 360
        val mapH = 200
        val x0 = width - mapW - pad
        val y0 = height - mapH - pad

        // фон карты
        g2.color = Color(10, 14, 18, 220)
        g2.fillRoundRect(x0, y0, mapW, mapH, 12, 12)
        g2.color = Color(90, 140, 110)
        g2.stroke = BasicStroke(2f)
        g2.drawRoundRect(x0, y0, mapW, mapH, 12, 12)
        g2.font = Font("Monospaced", Font.BOLD, 12)
        g2.drawString("КАРТА ОБЪЕКТА", x0 + 10, y0 + 18)

        // координаты комнат на карте (относительно x0,y0)
        // сцена сверху, два коридора по бокам, кухня посередине, офис снизу
        data class Room(val cam: core.Camera, val cx: Int, val cy: Int, val label: String, val dirX: Int, val dirY: Int)
        val rooms = listOf(
            Room(core.Camera.STAGE,     mapW / 2,       40,  "СЦЕНА",  0,  1),
            Room(core.Camera.HALL_LEFT, 70,             100, "Л.КОР.", 1,  0),
            Room(core.Camera.KITCHEN,   mapW / 2,       100, "КУХНЯ",  0, -1),
            Room(core.Camera.HALL_RIGHT,mapW - 70,      100, "П.КОР.", -1, 0),
            Room(core.Camera.OFFICE,    mapW / 2,       mapH - 40, "ОФИС", 0, -1)
        )

        // соединяющие коридоры (линии)
        g2.color = Color(50, 70, 55)
        g2.stroke = BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val idx = rooms.associateBy { it.cam }
        fun line(a: core.Camera, b: core.Camera) {
            val ra = idx[a] ?: return; val rb = idx[b] ?: return
            g2.drawLine(x0 + ra.cx, y0 + ra.cy, x0 + rb.cx, y0 + rb.cy)
        }
        line(core.Camera.STAGE, core.Camera.HALL_LEFT)
        line(core.Camera.STAGE, core.Camera.KITCHEN)
        line(core.Camera.STAGE, core.Camera.HALL_RIGHT)
        line(core.Camera.KITCHEN, core.Camera.OFFICE)
        line(core.Camera.HALL_LEFT, core.Camera.OFFICE)
        line(core.Camera.HALL_RIGHT, core.Camera.OFFICE)

        // точки камер с подписями и сектором обзора
        g2.font = Font("Monospaced", Font.BOLD, 10)
        for (r in rooms) {
            val cx = x0 + r.cx
            val cy = y0 + r.cy
            val active = r.cam == world.activeCamera

            // сектор обзора — треугольник по направлению
            val dLen = 26
            val dx = r.dirX * dLen
            val dy = r.dirY * dLen
            g2.color = Color(120, 200, 140, if (active) 180 else 80)
            val poly = java.awt.Polygon()
            poly.addPoint(cx, cy)
            poly.addPoint(cx + dx - r.dirY * 18, cy + dy + r.dirX * 18)
            poly.addPoint(cx + dx + r.dirY * 18, cy + dy - r.dirX * 18)
            g2.fillPolygon(poly)

            // точка камеры
            g2.color = if (active) Color(220, 60, 60) else Color(60, 100, 70)
            val rad = if (active) 8 else 6
            g2.fillOval(cx - rad, cy - rad, rad * 2, rad * 2)
            g2.color = Color(240, 240, 240)
            g2.drawString(r.cam.short, cx - 8, cy + 4)

            // подпись комнаты
            g2.color = if (active) Color(255, 200, 200) else Color(180, 200, 190)
            g2.drawString(r.label, cx - g2.fontMetrics.stringWidth(r.label) / 2, cy + 24)

            // Матвей в комнате — красная пульсация
            if (world.matveyOnCamera() == r.cam) {
                g2.color = Color(240, 50, 50, 200)
                g2.fillOval(cx + 10, cy - 14, 10, 10)
                g2.color = Color.WHITE
                g2.drawString("!", cx + 13, cy - 5)
            }
        }

        // подсказка
        g2.color = Color(150, 170, 160)
        g2.font = Font("SansSerif", Font.PLAIN, 10)
        g2.drawString("1..5 / клик — переход", x0 + 10, y0 + mapH - 8)
    }


    private fun drawHud(g2: Graphics2D) {
        g2.font = Font("Monospaced", Font.BOLD, 16)
        g2.color = Color.WHITE
        g2.drawString("НОЧЬ: ${world.config.name}", 20, 28)
        val p = world.power.toInt()
        g2.color = when { p > 50 -> Color(90, 230, 120); p > 20 -> Color(230, 210, 80); else -> Color(240, 80, 80) }
        g2.drawString("ЭНЕРГИЯ: $p%", 20, 50)
        g2.color = Color.WHITE
        val t = world.timeLeft.toInt()
        g2.drawString("ВРЕМЯ: %02d:%02d".format(t / 60, t % 60), 20, 72)
        g2.drawString("A/← левая · D/→ правая · N ночь · R рестарт · Esc меню", 20, height - 20)
    }

    private fun drawOverlays(g2: Graphics2D) {
        val scare = sprites["jumpscare.png"]
        if (world.jumpScare) {
            if (scare != null) g2.drawImage(scare, 0, 0, width, height, null)
            else { g2.color = Color(120, 0, 0, 220); g2.fillRect(0, 0, width, height) }
            g2.color = Color.WHITE
            g2.font = Font("SansSerif", Font.BOLD, 64)
            g2.drawString("МАТВЕЙ", (width - g2.fontMetrics.stringWidth("МАТВЕЙ")) / 2, height / 2)
            g2.font = Font("SansSerif", Font.PLAIN, 22)
            val s2 = "ЛКМ — рестарт. Esc — меню."
            g2.drawString(s2, (width - g2.fontMetrics.stringWidth(s2)) / 2, height / 2 + 50)
            return
        }
        if (world.won) {
            g2.color = Color(0, 0, 0, 200); g2.fillRect(0, 0, width, height)
            g2.color = Color(120, 240, 140)
            g2.font = Font("SansSerif", Font.BOLD, 42)
            val s = "6:00 — ТЫ ВЫЖИЛ"
            g2.drawString(s, (width - g2.fontMetrics.stringWidth(s)) / 2, height / 2 - 20)
            g2.color = Color.WHITE
            g2.font = Font("SansSerif", Font.PLAIN, 20)
            val s2 = if (nightIndex < 4) "N или ЛКМ — ${NightTable.byIndex(nightIndex + 1).name}. Esc — меню." else "Пятница пройдена. Esc — меню."
            g2.drawString(s2, (width - g2.fontMetrics.stringWidth(s2)) / 2, height / 2 + 30)
        }
    }
}

fun main() {
    SwingUtilities.invokeLater {
        AppFrame()
    }
}
