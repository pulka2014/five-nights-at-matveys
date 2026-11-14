package desktop

import core.Save
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
import javax.swing.JPanel

class MenuPanel(
    private val onNewGame: () -> Unit,
    private val onContinue: (Int) -> Unit
) : JPanel() {
    private var hovered = -1
    private var focused = 0
    private var hasSave = false
    private var saveNight = 0

    init {
        background = Color(12, 12, 16)
        isFocusable = true
        refresh()
        addMouseListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                hovered = hitTest(e.x, e.y)
                repaint()
            }
            override fun mousePressed(e: MouseEvent) {
                when (hitTest(e.x, e.y)) {
                    0 -> onNewGame()
                    1 -> if (hasSave) onContinue(saveNight)
                }
            }
        })
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_UP, KeyEvent.VK_W -> { focused = (focused + 1) % 2; repaint() }
                    KeyEvent.VK_DOWN, KeyEvent.VK_S -> { focused = (focused + 1) % 2; repaint() }
                    KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                        when (focused) {
                            0 -> onNewGame()
                            1 -> if (hasSave) onContinue(saveNight)
                        }
                    }
                }
            }
        })
    }

    fun refresh() {
        val d = Save.load()
        hasSave = d.nightIndex > 0 || d.finished
        saveNight = d.nightIndex
        repaint()
    }

    private fun btnNew() = Rectangle(width / 2 - 160, height / 2 - 30, 320, 56)
    private fun btnCont() = Rectangle(width / 2 - 160, height / 2 + 44, 320, 56)

    private fun hitTest(x: Int, y: Int): Int = when {
        btnNew().contains(x, y) -> 0
        btnCont().contains(x, y) -> 1
        else -> -1
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // фон
        g2.color = Color(12, 12, 16)
        g2.fillRect(0, 0, width, height)
        g2.color = Color(20, 24, 30)
        g2.fillRect(0, height / 2 - 200, width, 400)

        // заголовок
        g2.color = Color(200, 40, 40)
        g2.font = Font("Serif", Font.BOLD, 68)
        val title = "FIVE NIGHTS"
        val fm = g2.fontMetrics
        g2.drawString(title, (width - fm.stringWidth(title)) / 2, height / 2 - 140)
        g2.color = Color(240, 240, 240)
        g2.font = Font("Serif", Font.BOLD, 42)
        val sub = "AT MATVEY'S"
        g2.drawString(sub, (width - g2.fontMetrics.stringWidth(sub)) / 2, height / 2 - 95)

        // кнопки
        drawButton(g2, btnNew(), "НОВАЯ ИГРА", focused == 0, hovered == 0, true)
        drawButton(g2, btnCont(), if (hasSave) "ПРОДОЛЖИТЬ (${nightName(saveNight)})" else "ПРОДОЛЖИТЬ (нет сохранения)", focused == 1, hovered == 1, hasSave)

        // подсказка
        g2.color = Color(150, 150, 160)
        g2.font = Font("SansSerif", Font.PLAIN, 14)
        val hint = "↑↓ / W S — выбор · Enter — подтвердить · Esc в игре — выход в меню"
        g2.drawString(hint, (width - g2.fontMetrics.stringWidth(hint)) / 2, height - 30)
    }

    private fun drawButton(g2: Graphics2D, r: Rectangle, text: String, focused: Boolean, hovered: Boolean, enabled: Boolean) {
        val bg = when {
            !enabled -> Color(35, 35, 40)
            focused && hovered -> Color(120, 30, 30)
            focused -> Color(160, 40, 40)
            hovered -> Color(90, 30, 30)
            else -> Color(55, 30, 30)
        }
        g2.color = bg
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14)
        g2.color = if (enabled) Color(230, 220, 220) else Color(110, 110, 120)
        g2.font = Font("SansSerif", Font.BOLD, 20)
        val fm = g2.fontMetrics
        g2.drawString(text, r.x + (r.width - fm.stringWidth(text)) / 2, r.y + (r.height + fm.ascent - fm.descent) / 2)
        if (focused) {
            g2.color = Color(255, 80, 80)
            g2.drawRoundRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4, 16, 16)
        }
    }

    private fun nightName(i: Int): String = when (i) {
        0 -> "Пн"; 1 -> "Вт"; 2 -> "Ср"; 3 -> "Чт"; 4 -> "Пт"; else -> "?"
    }
}
