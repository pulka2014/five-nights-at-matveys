package com.pulka2014.fnaf

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import core.Door
import core.MatveyState
import core.World

class Main : ApplicationAdapter() {
    lateinit var batch: SpriteBatch
    lateinit var shape: ShapeRenderer
    lateinit var font: BitmapFont
    lateinit var world: World

    override fun create() {
        batch = SpriteBatch()
        shape = ShapeRenderer()
        font = BitmapFont()
        world = World()
        world.newNightHardReset(0)
    }

    override fun render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        world.update(Gdx.graphics.deltaTime)

        shape.begin(ShapeRenderer.ShapeType.Filled)
        
        // Офис (фон)
        shape.color = Color(0.15f, 0.15f, 0.2f, 1f)
        shape.rect(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        // Левая дверь
        shape.color = if (world.leftDoorClosed) Color.RED else Color.GREEN
        shape.rect(50f, Gdx.graphics.height / 2f - 150f, 100f, 300f)
        
        // Правая дверь
        shape.color = if (world.rightDoorClosed) Color.RED else Color.GREEN
        shape.rect(Gdx.graphics.width - 150f, Gdx.graphics.height / 2f - 150f, 100f, 300f)

        shape.end()

        batch.begin()
        font.draw(batch, "Energy: ${world.power.toInt()}%", 20f, Gdx.graphics.height - 20f)
        font.draw(batch, "Time: ${world.timeLeft.toInt()}", 20f, Gdx.graphics.height - 50f)
        
        if (world.matveyState == MatveyState.AT_DOOR) {
            font.draw(batch, "MATVEY AT DOOR!", Gdx.graphics.width / 2f - 50f, Gdx.graphics.height / 2f)
        }
        batch.end()

        if (Gdx.input.justTouched()) {
            val x = Gdx.input.x
            if (x < Gdx.graphics.width / 2f) {
                world.toggleDoor(Door.LEFT)
            } else {
                world.toggleDoor(Door.RIGHT)
            }
        }
    }

    override fun dispose() {
        batch.dispose()
        shape.dispose()
        font.dispose()
    }
}
