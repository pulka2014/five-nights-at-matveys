package com.pulka2014.fnaf

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

class MenuScreen(private val game: FnafGame) : Screen {
    private val batch = SpriteBatch()
    private val shape = ShapeRenderer()
    private val btnNewGame = Rectangle(0f, 0f, 400f, 90f)
    private val btnContinue = Rectangle(0f, 0f, 400f, 90f)
    private var hoveredIndex = -1

    override fun show() {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        btnNewGame.set(w / 2f - 200f, h / 2f - 30f, 400f, 90f)
        btnContinue.set(w / 2f - 200f, h / 2f - 150f, 400f, 90f)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val mx = Gdx.input.x.toFloat()
        val my = (Gdx.graphics.height - Gdx.input.y).toFloat()
        hoveredIndex = when {
            btnNewGame.contains(mx, my) -> 0
            btnContinue.contains(mx, my) -> 1
            else -> -1
        }

        // Заливка кнопок
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = if (hoveredIndex == 0) Color(0.7f, 0.15f, 0.15f, 1f) else Color(0.35f, 0.1f, 0.1f, 1f)
        shape.rect(btnNewGame.x, btnNewGame.y, btnNewGame.width, btnNewGame.height)
        shape.color = if (hoveredIndex == 1) Color(0.7f, 0.15f, 0.15f, 1f) else Color(0.35f, 0.1f, 0.1f, 1f)
        shape.rect(btnContinue.x, btnContinue.y, btnContinue.width, btnContinue.height)
        shape.end()

        // Рамки
        shape.begin(ShapeRenderer.ShapeType.Line)
        shape.color = Color.BLACK
        shape.rect(btnNewGame.x - 3f, btnNewGame.y - 3f, btnNewGame.width + 6f, btnNewGame.height + 6f)
        shape.rect(btnContinue.x - 3f, btnContinue.y - 3f, btnContinue.width + 6f, btnContinue.height + 6f)
        shape.end()
        shape.begin(ShapeRenderer.ShapeType.Line)
        shape.color = Color.WHITE
        shape.rect(btnNewGame.x + 2f, btnNewGame.y + 2f, btnNewGame.width - 4f, btnNewGame.height - 4f)
        shape.rect(btnContinue.x + 2f, btnContinue.y + 2f, btnContinue.width - 4f, btnContinue.height - 4f)
        shape.end()

        batch.begin()
        Assets.fontBig.color = Color.WHITE
        Assets.fontBig.draw(batch, "FIVE NIGHTS", Gdx.graphics.width / 2f - 200f, Gdx.graphics.height - 150f)
        Assets.fontBig.draw(batch, "AT MATVEY'S", Gdx.graphics.width / 2f - 210f, Gdx.graphics.height - 220f)
        
        Assets.font.color = Color.WHITE
        Assets.font.draw(batch, "NEW GAME", btnNewGame.x + 120f, btnNewGame.y + 55f)
        Assets.font.draw(batch, "CONTINUE", btnContinue.x + 120f, btnContinue.y + 55f)
        batch.end()

        if (Gdx.input.justTouched()) {
            when (hoveredIndex) {
                0 -> game.startNewGame()
                1 -> game.continueGame()
            }
        }
    }

    override fun resize(width: Int, height: Int) { show() }
    override fun pause() {}
    override fun resume() {}
    override fun hide() { dispose() }
    override fun dispose() { batch.dispose(); shape.dispose() }
}
