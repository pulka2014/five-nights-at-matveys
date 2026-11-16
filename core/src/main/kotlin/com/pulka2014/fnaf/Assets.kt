package com.pulka2014.fnaf

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont

object Assets {
    lateinit var office: Texture
    lateinit var matvey: Texture
    lateinit var roma: Texture
    lateinit var sonya: Texture
    lateinit var timofey: Texture
    var jumpscare: Texture? = null
    var romaScare: Texture? = null
    var sonyaScare: Texture? = null
    var timofeyScare: Texture? = null
    var doorClosed: Texture? = null
    var mask: Texture? = null
    var bomb: Texture? = null

    lateinit var soundStep: Sound
    lateinit var soundDoor: Sound
    lateinit var soundJumpscare: Sound
    lateinit var soundRomaScare: Sound
    lateinit var soundSonyaScare: Sound
    lateinit var soundTimofeyBang: Sound
    lateinit var soundTimofeyRun: Sound
    var ambientMusic: Music? = null

    val font: BitmapFont by lazy { BitmapFont().also { it.data.setScale(1.5f) } }
    val fontBig: BitmapFont by lazy { BitmapFont().also { it.data.setScale(3f) } }

    fun load() {
        office = Texture(Gdx.files.internal("sprites/office.png"))
        matvey = Texture(Gdx.files.internal("sprites/matvey.png"))
        roma = Texture(Gdx.files.internal("sprites/roma.png"))
        sonya = Texture(Gdx.files.internal("sprites/sonya.png"))
        timofey = Texture(Gdx.files.internal("sprites/timofey.png"))
        jumpscare = runCatching { Texture(Gdx.files.internal("sprites/jumpscare.png")) }.getOrNull()
        romaScare = runCatching { Texture(Gdx.files.internal("sprites/roma_scare.png")) }.getOrNull()
        sonyaScare = runCatching { Texture(Gdx.files.internal("sprites/sonya_scare.png")) }.getOrNull()
        timofeyScare = runCatching { Texture(Gdx.files.internal("sprites/timofey_scare.png")) }.getOrNull()
        doorClosed = runCatching { Texture(Gdx.files.internal("sprites/door_closed.png")) }.getOrNull()
        mask = runCatching { Texture(Gdx.files.internal("sprites/mask.png")) }.getOrNull()
        bomb = runCatching { Texture(Gdx.files.internal("sprites/bomb.png")) }.getOrNull()

        soundStep = Gdx.audio.newSound(Gdx.files.internal("sounds/step.wav"))
        soundDoor = Gdx.audio.newSound(Gdx.files.internal("sounds/door.wav"))
        soundJumpscare = Gdx.audio.newSound(Gdx.files.internal("sounds/jumpscare.wav"))
        soundRomaScare = runCatching { Gdx.audio.newSound(Gdx.files.internal("sounds/roma_scare.wav")) }.getOrNull() ?: soundJumpscare
        soundSonyaScare = runCatching { Gdx.audio.newSound(Gdx.files.internal("sounds/sonya_scare.wav")) }.getOrNull() ?: soundJumpscare
        soundTimofeyBang = runCatching { Gdx.audio.newSound(Gdx.files.internal("sounds/timofey_bang.wav")) }.getOrNull() ?: soundDoor
        soundTimofeyRun = runCatching { Gdx.audio.newSound(Gdx.files.internal("sounds/run.wav")) }.getOrNull() ?: soundDoor
        ambientMusic = runCatching { Gdx.audio.newMusic(Gdx.files.internal("sounds/ambient.wav")) }.getOrNull()

        font.color = Color.WHITE
        fontBig.color = Color.WHITE
    }

    fun dispose() {
        office.dispose(); matvey.dispose(); roma.dispose(); sonya.dispose(); timofey.dispose()
        jumpscare?.dispose(); romaScare?.dispose(); sonyaScare?.dispose(); timofeyScare?.dispose()
        doorClosed?.dispose(); mask?.dispose(); bomb?.dispose()
        soundStep.dispose(); soundDoor.dispose(); soundJumpscare.dispose()
        soundRomaScare.dispose(); soundSonyaScare.dispose(); soundTimofeyBang.dispose(); soundTimofeyRun.dispose()
        ambientMusic?.dispose()
        font.dispose(); fontBig.dispose()
    }
}
