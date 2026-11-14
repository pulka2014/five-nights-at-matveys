package core

import kotlin.random.Random

class World(val width: Float = 960f, val height: Float = 640f) {
    var config: NightConfig = NightTable.byIndex(0)
    var timeLeft: Float = 300f
    var power: Float = 100f
    val nightLength: Float = 300f
    var leftDoorClosed = false
    var rightDoorClosed = false
    var gameOver = false
    var won = false
    var jumpScare = false
    var matveyState = MatveyState.HIDDEN
    var matveyDoor: Door = Door.LEFT
    var matveyTimer = 0f
    var matveyPatienceLeft = 0f
    var activeCamera: Camera = Camera.OFFICE

    private val rand = Random(System.nanoTime())
    private var lastStepAt = 0f
    private var ambientCooldown = 0f
    private var approachCount = 0
    private var stepsBurstDone = false

    init {
        Sound.preload(listOf(
            "jumpscare.wav" to 0L,
            "step.wav" to 300L,
            "door.wav" to 0L,
            "ambient.wav" to 0L
        ))
    }

    fun toggleDoor(d: Door) {
        if (gameOver || won) return
        when (d) {
            Door.LEFT -> leftDoorClosed = !leftDoorClosed
            Door.RIGHT -> rightDoorClosed = !rightDoorClosed
        }
        Sound.playOnce("door.wav", volumeDb = 0f)
    }

    fun newNightHardReset(index: Int) {
        config = NightTable.byIndex(index)
        timeLeft = nightLength
        power = 100f
        leftDoorClosed = false
        rightDoorClosed = false
        gameOver = false
        won = false
        jumpScare = false
        matveyState = MatveyState.HIDDEN
        matveyDoor = if (rand.nextBoolean()) Door.LEFT else Door.RIGHT
        matveyTimer = 0f
        matveyPatienceLeft = 0f
        lastStepAt = 0f
        approachCount = 0
        activeCamera = Camera.OFFICE
        Sound.stopAll()
        Sound.playOnce("ambient.wav", volumeDb = -15f)
        ambientCooldown = 5f + rand.nextFloat() * 25f
        stepsBurstDone = false
    }

    /** На какой камере сейчас виден Матвей (null — нигде). */
    fun matveyOnCamera(): Camera? = when (matveyState) {
        MatveyState.HIDDEN -> Camera.STAGE
        MatveyState.APPROACHING -> when (matveyDoor) {
            Door.LEFT -> Camera.HALL_LEFT
            Door.RIGHT -> Camera.HALL_RIGHT
        }
        MatveyState.AT_DOOR, MatveyState.RETREATING -> when (matveyDoor) {
            Door.LEFT -> Camera.HALL_LEFT
            Door.RIGHT -> Camera.HALL_RIGHT
        }
    }

    fun switchCamera(c: Camera) { activeCamera = c }

    private fun scheduleNextApproach() {
        matveyState = MatveyState.HIDDEN
        matveyTimer = 0f
        matveyDoor = if (rand.nextBoolean()) Door.LEFT else Door.RIGHT
    }

    private fun triggerJumpScare() {
        gameOver = true
        jumpScare = true
        Sound.stop("ambient.wav")
        Sound.playOnce("jumpscare.wav", volumeDb = 0f)
    }

    fun update(dt: Float) {
        if (gameOver || won) return

        timeLeft -= dt
        if (timeLeft <= 0f) {
            timeLeft = 0f
            won = true
            Sound.stop("ambient.wav")
            return
        }

        var drain = 0.4778f * config.drainMultiplier
        if (leftDoorClosed) drain += 0.6f
        if (rightDoorClosed) drain += 0.6f
        if (!leftDoorClosed && !rightDoorClosed) drain -= 0.2f
        power = (power - drain * dt).coerceIn(0f, 100f)
        if (power <= 0f) {
            leftDoorClosed = false
            rightDoorClosed = false
            triggerJumpScare()
            return
        }

        // эмбиент: один проход, затем пауза 5..30 с
        if (ambientCooldown > 0f) {
            ambientCooldown -= dt
            if (ambientCooldown <= 0f) {
                Sound.playOnce("ambient.wav", volumeDb = -15f)
                ambientCooldown = 5f + rand.nextFloat() * 25f
            }
        }

        matveyTimer += dt * config.speedMultiplier
        when (matveyState) {
            MatveyState.HIDDEN -> {
                if (matveyTimer >= 6f + rand.nextFloat() * 6f) {
                    matveyState = MatveyState.APPROACHING
                    matveyTimer = 0f
                    approachCount++
                    if (approachCount == 1) {
                        Sound.playOnce("step.wav", volumeDb = 6f)
                    }
                }
            }
            MatveyState.APPROACHING -> {
                if (!stepsBurstDone && matveyTimer - lastStepAt >= 0.6f) {
                    lastStepAt = matveyTimer
                    Sound.playOnce("step.wav", volumeDb = 6f)
                }
                if (matveyTimer >= config.approachSeconds) {
                    matveyState = MatveyState.AT_DOOR
                    matveyTimer = 0f
                    matveyPatienceLeft = (3.2f - config.speedMultiplier * 0.4f).coerceAtLeast(1.2f)
                    stepsBurstDone = true
                }
            }
            MatveyState.AT_DOOR -> {
                val doorClosed = when (matveyDoor) {
                    Door.LEFT -> leftDoorClosed
                    Door.RIGHT -> rightDoorClosed
                }
                if (doorClosed) {
                    matveyState = MatveyState.RETREATING
                    matveyTimer = 0f
                } else {
                    matveyPatienceLeft -= dt
                    if (matveyPatienceLeft <= 0f) triggerJumpScare()
                }
            }
            MatveyState.RETREATING -> {
                if (matveyTimer >= 4f) scheduleNextApproach()
            }
        }
    }
}
