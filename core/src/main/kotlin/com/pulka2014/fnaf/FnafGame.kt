package com.pulka2014.fnaf

import com.badlogic.gdx.Game
import core.Save

class FnafGame(val videoLauncher: VideoLauncher? = null) : Game() {
    override fun create() {
        Assets.load()
        setScreen(MenuScreen(this))
    }

    fun startNewGame() {
        Save.reset()
        Save.save(0, false)
        setScreen(GameScreen(this))
    }

    fun continueGame() {
        val data = Save.load()
        setScreen(GameScreen(this))
    }

    fun playBombVideo() {
        videoLauncher?.playBomb()
    }

    override fun dispose() {
        Assets.dispose()
        super.dispose()
    }
}
