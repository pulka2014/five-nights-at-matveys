package com.pulka2014.fnaf

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import core.Camera
import core.Door
import core.MatveyState
import core.World

class GameScreen(private val game: FnafGame? = null) : Screen {
    private val WORLD_W = 960f
    private val WORLD_H = 640f

    private val camera = OrthographicCamera()
    private val viewport: Viewport = FitViewport(WORLD_W, WORLD_H, camera)
    private val batch = SpriteBatch().apply { projectionMatrix = camera.combined }
    private val shape = ShapeRenderer()
    private val world = World().also { it.newNightHardReset(0) }

    private var lastStepSound = 0f
    private var camMode = false
    private val btnCam = Rectangle(0f, 0f, 160f, 70f)
    private val btnMask = Rectangle(0f, 0f, 160f, 70f)
    private val btnBurger = Rectangle(0f, 0f, 0f, 0f)
    private val camButtons = arrayOfNulls<Rectangle>(5)
    private var lastJumpScareState = false
    private var lastWonState = false

    private val camNames = arrayOf("DINING", "L.HALL", "R.HALL", "KITCHEN", "STAGE")

    override fun show() {
        btnCam.set(WORLD_W - 180f, 20f, 160f, 70f)
        btnMask.set(WORLD_W - 180f, 100f, 160f, 70f)
        val btnW = 140f; val btnH = 70f; val gap = 15f
        val totalW = 5 * btnW + 4 * gap
        val startX = (WORLD_W - totalW) / 2f
        for (i in 0 until 5) camButtons[i] = Rectangle(startX + i * (btnW + gap), 20f, btnW, btnH)
        Assets.ambientMusic?.let { it.isLooping = true; it.volume = 0.3f; it.play() }
    }

    override fun render(delta: Float) {
        camera.update()
        batch.projectionMatrix = camera.combined
        shape.projectionMatrix = camera.combined

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        world.update(delta)

        if (world.won) { drawWonScreen(); return }
        if (world.jumpScare) {
            if (!lastJumpScareState) {
                Assets.ambientMusic?.stop()
                if (world.whoScared == "bomb") game?.playBombVideo()
                val snd = when (world.whoScared) {
                    "roma" -> Assets.soundRomaScare
                    "sonya" -> Assets.soundSonyaScare
                    "timofey" -> Assets.soundTimofeyBang
                    else -> Assets.soundJumpscare
                }
                snd.play(1f)
                lastJumpScareState = true
                Thread { Thread.sleep(3000); snd.stop() }.apply { isDaemon = true }.start()
            }
            drawJumpScare()
            if (Gdx.input.justTouched()) {
                Assets.soundJumpscare.stop()
                Assets.soundRomaScare.stop()
                Assets.soundSonyaScare.stop()
                Assets.soundTimofeyBang.stop()
                world.newNightHardReset(world.config.nightIndex)
                lastJumpScareState = false
                show()
            }
            return
        }

        if (camMode) drawCameras() else drawOffice()
        if (world.maskOn) drawMask()
        drawHudAndButtons()
        handleInput()
        playSounds(delta)
    }

    private fun drawWonScreen() {
        if (!lastWonState) { lastWonState = true; Assets.ambientMusic?.stop() }
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.85f)
        shape.rect(0f, 0f, WORLD_W, WORLD_H)
        shape.end()
        batch.begin()
        Assets.fontBig.color = Color(0.5f, 1f, 0.5f, 1f)
        Assets.fontBig.draw(batch, "6:00 AM", WORLD_W / 2f - 130f, WORLD_H / 2f + 50f)
        Assets.font.color = Color.WHITE
        Assets.font.draw(batch, "YOU SURVIVED", WORLD_W / 2f - 90f, WORLD_H / 2f)
        Assets.font.draw(batch, "Tap to continue", WORLD_W / 2f - 90f, WORLD_H / 2f - 60f)
        batch.end()
        if (Gdx.input.justTouched()) {
            world.newNightHardReset((world.config.nightIndex + 1).coerceAtMost(4))
            lastWonState = false
            show()
        }
    }

    private fun drawOffice() {
        batch.begin()
        batch.draw(Assets.office, 0f, 0f, WORLD_W, WORLD_H)

        val doorTex = Assets.doorClosed ?: Assets.office
        val doorW = WORLD_W * 0.15f
        val doorH = WORLD_H
        if (world.leftDoorClosed) {
            val region = TextureRegion(doorTex); region.flip(true, false)
            batch.draw(region, 0f, 0f, doorW, doorH)
        }
        if (world.rightDoorClosed) {
            batch.draw(doorTex, WORLD_W - doorW, 0f, doorW, doorH)
        }

        if (world.matveyState == MatveyState.AT_DOOR) {
            val alpha = if ((world.matveyDoor == Door.LEFT && world.leftDoorClosed) ||
                            (world.matveyDoor == Door.RIGHT && world.rightDoorClosed)) 0.35f else 1f
            batch.setColor(1f, 1f, 1f, alpha)
            val w = 280f; val h = 360f
            val x = if (world.matveyDoor == Door.LEFT) 0f else WORLD_W - w
            val y = WORLD_H / 2f - h / 2f
            if (world.matveyDoor == Door.RIGHT) {
                val region = TextureRegion(Assets.matvey); region.flip(true, false)
                batch.draw(region, x, y, w, h)
            } else batch.draw(Assets.matvey, x, y, w, h)
            batch.setColor(Color.WHITE)
        }

        if (world.sonyaAtDoor) {
            val w = 300f; val h = 380f
            val alpha = if (world.maskOn) 0.5f else 1f
            batch.setColor(1f, 1f, 1f, alpha)
            if (world.sonyaScanTimer < 1.5f) batch.draw(Assets.sonya, WORLD_W - w, WORLD_H / 2f - h / 2f, w, h)
            else {
                val sx = world.sonyaSlideX * (WORLD_W - w)
                batch.draw(Assets.sonya, sx, WORLD_H / 2f - h / 2f, w, h)
            }
            batch.setColor(Color.WHITE)
        }

        if (world.timofeyAtDoor) {
            val w = 260f; val h = 340f
            val alpha = if (world.rightDoorClosed) 0.4f else 1f
            batch.setColor(1f, 1f, 1f, alpha)
            batch.draw(Assets.timofey, WORLD_W - w, WORLD_H / 2f - h / 2f, w, h)
            batch.setColor(Color.WHITE)
        }

        if (world.romaAngry) {
            val w = 260f; val h = 340f
            batch.draw(Assets.roma, 0f, WORLD_H / 2f - h / 2f, w, h)
        }
        batch.end()
    }

    private fun drawMask() {
        batch.begin()
        val m = Assets.mask
        if (m != null) batch.draw(m, 0f, 0f, WORLD_W, WORLD_H)
        else {
            batch.setColor(0.5f, 0.5f, 0.5f, 0.6f)
            batch.draw(Assets.office, 0f, 0f, WORLD_W, WORLD_H)
            batch.setColor(Color.WHITE)
        }
        if (world.sonyaAtDoor) {
            val w = 300f; val h = 380f
            batch.setColor(1f, 1f, 1f, 0.5f)
            val sx = if (world.sonyaScanTimer < 1.5f) WORLD_W - w
                     else world.sonyaSlideX * (WORLD_W - w)
            batch.draw(Assets.sonya, sx, WORLD_H / 2f - h / 2f, w, h)
            batch.setColor(Color.WHITE)
        }
        batch.end()
    }

    private fun drawCameras() {
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.05f, 0.08f, 0.05f, 1f)
        shape.rect(0f, 0f, WORLD_W, WORLD_H)
        shape.color = Color(0.2f, 0.3f, 0.2f, 0.5f)
        for (x in 0..WORLD_W.toInt() step 100) shape.rectLine(x.toFloat(), 0f, x.toFloat(), WORLD_H, 2f)
        for (y in 0..WORLD_H.toInt() step 100) shape.rectLine(0f, y.toFloat(), WORLD_W, y.toFloat(), 2f)

        when (world.activeCamera) {
            Camera.DINING -> {
                shape.color = Color(0.25f, 0.2f, 0.15f, 1f)
                shape.rect(100f, 150f, WORLD_W - 200f, WORLD_H - 300f)
            }
            Camera.HALL_LEFT, Camera.HALL_RIGHT -> {
                val cx = WORLD_W / 2f
                shape.color = Color(0.25f, 0.25f, 0.3f, 1f)
                shape.triangle(cx - 200f, 150f, cx + 200f, 150f, cx, WORLD_H - 150f)
            }
            Camera.KITCHEN -> {
                shape.color = Color(0.3f, 0.35f, 0.4f, 1f)
                shape.rect(150f, 200f, 300f, 150f)
            }
            Camera.STAGE -> {
                shape.color = Color(0.4f, 0.1f, 0.1f, 1f)
                shape.rect(100f, WORLD_H - 200f, WORLD_W - 200f, 100f)
            }
        }
        shape.end()

        batch.begin()
        Assets.font.color = Color(0.7f, 1f, 0.7f, 1f)
        Assets.font.draw(batch, world.activeCamera.label, 20f, WORLD_H - 20f)
        if (world.matveyOnCamera() == world.activeCamera) {
            val mw = 200f; val mh = 260f
            batch.setColor(1f, 1f, 1f, 0.7f)
            batch.draw(Assets.matvey, WORLD_W / 2f - mw / 2f, WORLD_H / 2f - mh / 2f, mw, mh)
            batch.setColor(Color.WHITE)
        }
        if (world.activeCamera == Camera.DINING) {
            val rw = 240f; val rh = 300f
            batch.draw(Assets.roma, 100f, WORLD_H / 2f - rh / 2f, rw, rh)
        }
        batch.end()

        if (world.activeCamera == Camera.DINING) {
            val bw = WORLD_W * 0.6f
            val bh = 110f
            val bx = WORLD_W / 2f - bw / 2f
            val by = WORLD_H * 0.75f
            btnBurger.set(bx, by, bw, bh)
            shape.begin(ShapeRenderer.ShapeType.Filled)
            shape.color = Color(0.9f, 0.6f, 0.1f, 1f)
            shape.rect(bx, by, bw, bh)
            shape.end()
            shape.begin(ShapeRenderer.ShapeType.Line)
            shape.color = Color.BLACK
            shape.rect(bx - 3f, by - 3f, bw + 6f, bh + 6f)
            shape.end()
            shape.begin(ShapeRenderer.ShapeType.Line)
            shape.color = Color.WHITE
            shape.rect(bx + 2f, by + 2f, bw - 4f, bh - 4f)
            shape.end()
            batch.begin()
            Assets.font.color = Color.WHITE
            Assets.font.draw(batch, "GIVE BURGER (#${world.burgerCount})", bx + 20f, by + 60f)
            batch.end()
        }
    }

    private fun drawHudAndButtons() {
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = if (camMode) Color(0.8f, 0.2f, 0.2f, 1f) else Color(0.2f, 0.6f, 0.2f, 1f)
        shape.rect(btnCam.x, btnCam.y, btnCam.width, btnCam.height)
        shape.color = if (world.maskOn) Color(0.2f, 0.4f, 0.8f, 1f) else Color(0.3f, 0.3f, 0.4f, 1f)
        shape.rect(btnMask.x, btnMask.y, btnMask.width, btnMask.height)
        if (camMode) for (i in 0 until 5) {
            val b = camButtons[i] ?: continue
            shape.color = if (world.activeCamera.id == i + 1) Color(0.9f, 0.25f, 0.25f, 1f) else Color(0.2f, 0.45f, 0.25f, 1f)
            shape.rect(b.x, b.y, b.width, b.height)
        }
        shape.end()

        shape.begin(ShapeRenderer.ShapeType.Line)
        shape.color = Color.BLACK
        shape.rect(btnCam.x - 3f, btnCam.y - 3f, btnCam.width + 6f, btnCam.height + 6f)
        shape.rect(btnMask.x - 3f, btnMask.y - 3f, btnMask.width + 6f, btnMask.height + 6f)
        if (camMode) for (i in 0 until 5) {
            val b = camButtons[i] ?: continue
            shape.rect(b.x - 3f, b.y - 3f, b.width + 6f, b.height + 6f)
        }
        shape.end()

        shape.begin(ShapeRenderer.ShapeType.Line)
        shape.color = Color.WHITE
        shape.rect(btnCam.x + 2f, btnCam.y + 2f, btnCam.width - 4f, btnCam.height - 4f)
        shape.rect(btnMask.x + 2f, btnMask.y + 2f, btnMask.width - 4f, btnMask.height - 4f)
        if (camMode) for (i in 0 until 5) {
            val b = camButtons[i] ?: continue
            shape.rect(b.x + 2f, b.y + 2f, b.width - 4f, b.height - 4f)
        }
        shape.end()

        // Оранжевый значок опасности, если Рома голоден
        if (world.romaHungry) {
            val cx = WORLD_W - 200f
            val cy = WORLD_H - 80f
            shape.begin(ShapeRenderer.ShapeType.Filled)
            shape.color = Color(1f, 0.55f, 0f, 1f)  // оранжевый
            shape.triangle(cx - 20f, cy - 25f, cx + 20f, cy - 25f, cx, cy + 25f)
            shape.end()
            batch.begin()
            Assets.fontBig.color = Color.WHITE
            Assets.fontBig.draw(batch, "!", cx - 10f, cy + 10f)
            batch.end()
        }

        batch.begin()
        Assets.font.color = Color.WHITE
        Assets.font.draw(batch, "Energy: ${world.power.toInt()}%", 20f, WORLD_H - 20f)
        Assets.font.draw(batch, "Time: ${world.timeLeft.toInt()}", 20f, WORLD_H - 50f)
        Assets.font.draw(batch, if (camMode) "OFFICE" else "CAM", btnCam.x + 45f, btnCam.y + 45f)
        Assets.font.draw(batch, if (world.maskOn) "MASK ON" else "MASK", btnMask.x + 35f, btnMask.y + 45f)
        if (camMode) for (i in 0 until 5) {
            val b = camButtons[i] ?: continue
            Assets.font.draw(batch, camNames[i], b.x + 10f, b.y + b.height - 15f)
            Assets.font.draw(batch, "${i + 1}", b.x + 10f, b.y + 25f)
        }
        batch.end()
    }

    private fun drawJumpScare() {
        batch.begin()
        val tex = when (world.whoScared) {
            "roma" -> Assets.romaScare ?: Assets.roma
            "sonya" -> Assets.sonyaScare ?: Assets.sonya
            "timofey" -> Assets.timofeyScare ?: Assets.timofey
            "bomb" -> Assets.bomb ?: Assets.office
            else -> Assets.jumpscare ?: Assets.matvey
        }
        batch.draw(tex, 0f, 0f, WORLD_W, WORLD_H)
        Assets.fontBig.color = Color.WHITE
        val name = when (world.whoScared) {
            "roma" -> "ROMA"
            "sonya" -> "SONYA"
            "timofey" -> "TIMOFEY"
            "bomb" -> "NUCLEAR EXPLOSION"
            else -> "MATVEY"
        }
        Assets.fontBig.draw(batch, name, WORLD_W / 2f - 100f, WORLD_H / 2f + 50f)
        Assets.font.draw(batch, "Tap to restart", WORLD_W / 2f - 80f, WORLD_H / 2f - 50f)
        batch.end()
    }

    private fun handleInput() {
        if (Gdx.input.justTouched()) {
            val vec = com.badlogic.gdx.math.Vector3(
                Gdx.input.x.toFloat(),
                Gdx.input.y.toFloat(),
                0f
            )
            viewport.unproject(vec)
            val x = vec.x
            val y = vec.y

            if (btnCam.contains(x, y)) {
                if (!world.maskOn) {
                    camMode = !camMode
                    world.tabletOpen = camMode
                }
                return
            }
            if (btnMask.contains(x, y)) { if (!camMode) world.toggleMask(); return }
            if (camMode && world.activeCamera == Camera.DINING && btnBurger.contains(x, y)) {
                world.giveBurger(); return
            }
            if (camMode) {
                for (i in 0 until 5) {
                    val b = camButtons[i] ?: continue
                    if (b.contains(x, y)) {
                        val cams = Camera.values()
                        if (i < cams.size) world.switchCamera(cams[i])
                        return
                    }
                }
            } else {
                val wasLeft = world.leftDoorClosed
                val wasRight = world.rightDoorClosed
                if (x < WORLD_W / 2f) world.toggleDoor(Door.LEFT)
                else world.toggleDoor(Door.RIGHT)
                if (world.leftDoorClosed != wasLeft || world.rightDoorClosed != wasRight) Assets.soundDoor.play(0.8f)
            }
        }
    }

    private fun playSounds(delta: Float) {
        if (world.timofeyRunning) {
            world.timofeyRunning = false
            Assets.soundTimofeyRun.play(1f)
            println("[FNAF] PLAY RUN SOUND (running)")
        }
        if (world.matveyState == MatveyState.APPROACHING) {
            lastStepSound += delta
            if (lastStepSound > 0.6f) { Assets.soundStep.play(1f); lastStepSound = 0f }
        }
    }

    override fun resize(width: Int, height: Int) { viewport.update(width, height, true); show() }
    override fun pause() { Assets.ambientMusic?.pause() }
    override fun resume() { Assets.ambientMusic?.play() }
    override fun hide() { Assets.ambientMusic?.stop(); dispose() }
    override fun dispose() { batch.dispose(); shape.dispose() }
}
