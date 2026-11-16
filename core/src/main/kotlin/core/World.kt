package core

import kotlin.random.Random

class World(val width: Float = 960f, val height: Float = 640f) {
    private val rand = Random(System.nanoTime())
    var config: NightConfig = NightTable.byIndex(0)
    var timeLeft: Float = 300f
    var power: Float = 100f
    var leftDoorClosed = false
    var rightDoorClosed = false
    var gameOver = false
    var won = false
    var jumpScare = false
    var activeCamera: Camera = Camera.DINING
    var tabletOpen = false
    var maskOn = false

    var matveyState = MatveyState.HIDDEN
    var matveyDoor: Door = Door.LEFT
    var matveyTimer = 0f
    var matveyPatienceLeft = 0f
    var approachCount = 0

    var romaHungry = false
    var romaTimer = 0f
    var romaAngry = false
    var romaDoorTimer = 0f
    var burgerCount = 0

    var sonyaAtDoor = false
    var sonyaScanTimer = 0f
    var sonyaCooldown = 20f + rand.nextFloat() * 20f
    var sonyaSlideX: Float = 0.5f
    var sonyaSlideDir: Float = 1f

    var timofeyAtDoor = false
    var timofeyJustAppeared = false
    var timofeyRunning = false
    var timofeyRunTimer = 0f
    var timofeyRunPlayed = false
    var timofeyWaitClosed: Float = 6f
    var timofeyWaitOpen: Float = 2f
    var timofeyTimer = 0f
    var timofeyCooldown = 50f + rand.nextFloat() * 25f
    var timofeyHits = 0

    var whoScared: String = "matvey"
    var bombTriggered = false
    private var penaltyAccum = 0f

    fun toggleDoor(d: Door) {
        if (gameOver || won) return
        when (d) {
            Door.LEFT -> if (!romaAngry) leftDoorClosed = !leftDoorClosed
            Door.RIGHT -> if (!sonyaAtDoor) rightDoorClosed = !rightDoorClosed
        }
    }

    fun newNightHardReset(index: Int) {
        config = NightTable.byIndex(index)
        timeLeft = 300f; power = 100f
        leftDoorClosed = false; rightDoorClosed = false
        gameOver = false; won = false; jumpScare = false
        tabletOpen = false; maskOn = false
        matveyState = MatveyState.HIDDEN
        matveyDoor = if (rand.nextBoolean()) Door.LEFT else Door.RIGHT
        matveyTimer = 0f; matveyPatienceLeft = 0f; approachCount = 0
        romaHungry = false; romaTimer = 0f; romaAngry = false; romaDoorTimer = 0f; burgerCount = 0
        sonyaAtDoor = false; sonyaScanTimer = 0f; sonyaCooldown = 20f + rand.nextFloat() * 20f
        sonyaSlideX = 0.5f; sonyaSlideDir = 1f
        timofeyAtDoor = false; timofeyTimer = 0f; timofeyCooldown = 50f + rand.nextFloat() * 25f; timofeyHits = 0; timofeyJustAppeared = false; timofeyRunning = false; timofeyRunTimer = 0f; timofeyRunPlayed = false
        whoScared = "matvey"; bombTriggered = false; penaltyAccum = 0f
    }

    fun giveBurger() {
        romaHungry = false
        romaTimer = 0f
        burgerCount++
        println("[FNAF] BURGER #$burgerCount")
        if (burgerCount > 3 && !bombTriggered) {
            bombTriggered = true
            gameOver = true
            jumpScare = true
            whoScared = "bomb"
        }
    }

    fun toggleMask() { if (!gameOver && !won && !tabletOpen) maskOn = !maskOn }
    fun toggleCamera() { if (!gameOver && !won && !maskOn) tabletOpen = !tabletOpen }
    fun switchCamera(c: Camera) { activeCamera = c }

    fun matveyOnCamera(): Camera? = when (matveyState) {
        MatveyState.HIDDEN -> Camera.STAGE
        MatveyState.DINING -> Camera.DINING
        MatveyState.APPROACHING -> if (matveyDoor == Door.LEFT) Camera.HALL_LEFT else Camera.HALL_RIGHT
        MatveyState.AT_DOOR, MatveyState.RETREATING -> if (matveyDoor == Door.LEFT) Camera.HALL_LEFT else Camera.HALL_RIGHT
    }

    fun update(dt: Float) {
        if (gameOver || won) return
        timeLeft -= dt
        if (timeLeft <= 0f) { timeLeft = 0f; won = true; return }

        val baseDrain = when (config.nightIndex) {
            0 -> 0.333f; 1 -> 0.37f; 2 -> 0.40f; 3 -> 0.42f; else -> 0.45f
        }
        var drain = baseDrain
        if (leftDoorClosed) drain += 0.20f
        if (rightDoorClosed) drain += 0.20f
        if (tabletOpen) drain += 0.15f
        if (maskOn) drain += 0.10f
        // В 1.5 раза меньше (÷ 1.5 = × 0.6667) для всех ночей
        drain *= 0.6667f
        if (config.nightIndex >= 1) {
            val interval = when (config.nightIndex) { 1 -> 6f; 2 -> 5f; 3 -> 4f; else -> 3f }
            penaltyAccum += dt
            if (penaltyAccum >= interval) { penaltyAccum = 0f; power -= 0.1f }
        }
        power = (power - drain * dt).coerceIn(0f, 100f)
        if (power <= 0f) { gameOver = true; jumpScare = true; whoScared = "matvey"; return }

        matveyTimer += dt * config.speedMultiplier
        when (matveyState) {
            MatveyState.HIDDEN -> if (matveyTimer >= 4f + rand.nextFloat() * 10f) { matveyState = MatveyState.DINING; matveyTimer = 0f }
            MatveyState.DINING -> if (matveyTimer >= 3f + rand.nextFloat() * 5f) { matveyState = MatveyState.APPROACHING; matveyTimer = 0f; approachCount++ }
            MatveyState.APPROACHING -> if (matveyTimer >= config.approachSeconds) {
                matveyState = MatveyState.AT_DOOR; matveyTimer = 0f
                matveyPatienceLeft = (3.2f - config.speedMultiplier * 0.4f).coerceAtLeast(1.2f)
            }
            MatveyState.AT_DOOR -> {
                val closed = if (matveyDoor == Door.LEFT) leftDoorClosed else rightDoorClosed
                if (closed) { matveyState = MatveyState.RETREATING; matveyTimer = 0f }
                else { matveyPatienceLeft -= dt; if (matveyPatienceLeft <= 0f) { gameOver = true; jumpScare = true; whoScared = "matvey" } }
            }
            MatveyState.RETREATING -> if (matveyTimer >= 3f + rand.nextFloat() * 3f) {
                matveyState = MatveyState.HIDDEN; matveyTimer = 0f
                matveyDoor = if (rand.nextBoolean()) Door.LEFT else Door.RIGHT
            }
        }

        if (!romaAngry) {
            romaTimer += dt
            if (!romaHungry && romaTimer >= 25f + rand.nextFloat() * 15f) {
                romaHungry = true; romaTimer = 0f
                burgerCount = 0
                println("[FNAF] ROMA IS HUNGRY (burger count reset)")
            } else if (romaHungry && romaTimer >= 25f + rand.nextFloat() * 15f) {
                romaAngry = true; romaDoorTimer = 0f; romaTimer = 0f
                println("[FNAF] ROMA IS ANGRY")
            }
        } else {
            romaDoorTimer += dt
            if (romaDoorTimer >= 3f) { gameOver = true; jumpScare = true; whoScared = "roma" }
        }

        if (sonyaAtDoor) {
            sonyaScanTimer += dt
            if (sonyaScanTimer > 1.5f) {
                sonyaSlideX += sonyaSlideDir * dt * 0.3f
                if (sonyaSlideX > 0.8f) { sonyaSlideX = 0.8f; sonyaSlideDir = -1f }
                if (sonyaSlideX < 0.2f) { sonyaSlideX = 0.2f; sonyaSlideDir = 1f }
            }
            if (!maskOn && sonyaScanTimer >= 5f) { gameOver = true; jumpScare = true; whoScared = "sonya" }
            if (maskOn && sonyaScanTimer >= 6f) {
                sonyaAtDoor = false; sonyaScanTimer = 0f; sonyaCooldown = 20f + rand.nextFloat() * 20f; sonyaSlideX = 0.5f
            }
        } else {
            sonyaCooldown -= dt
            if (sonyaCooldown <= 0f && !(matveyState == MatveyState.AT_DOOR && matveyDoor == Door.RIGHT)) {
                sonyaAtDoor = true; sonyaScanTimer = 0f; sonyaSlideX = 0.5f; sonyaSlideDir = 1f
                rightDoorClosed = false  // дверь автоматически открывается
                println("[FNAF] SONYA AT DOOR (door auto-opened)")
            }
        }

        if (timofeyAtDoor) {
            timofeyTimer += dt
            if (rightDoorClosed) {
                if (timofeyTimer >= 1f) {
                    timofeyAtDoor = false; timofeyTimer = 0f; timofeyCooldown = 50f + rand.nextFloat() * 25f; timofeyRunPlayed = false
                    println("[FNAF] TIMOFEY LEFT")
                }
            } else {
                if (timofeyTimer >= 1f) {
                    timofeyHits++
                    power = (power - (timofeyHits * 5 + 1)).coerceAtLeast(0f)
                    gameOver = true; jumpScare = true; whoScared = "timofey"
                    timofeyAtDoor = false; timofeyTimer = 0f; timofeyCooldown = 50f + rand.nextFloat() * 25f; timofeyRunPlayed = false
                    println("[FNAF] TIMOFEY ENTERED")
                }
            }
        } else {
            timofeyCooldown -= dt
            // За 1.5 секунды до появления у двери — Тимофей начинает бежать (один раз)
            if (timofeyCooldown <= 1.5f && !timofeyRunPlayed) {
                timofeyRunPlayed = true
                timofeyRunning = true
                println("[FNAF] TIMOFEY RUNNING")
            }
            if (timofeyCooldown <= 0f && !(matveyState == MatveyState.AT_DOOR && matveyDoor == Door.RIGHT) && !sonyaAtDoor) {
                timofeyAtDoor = true; timofeyTimer = 0f
                timofeyWaitClosed = (5f + rand.nextFloat() * 2f) * 5f  // 25-35 сек (×5)
                timofeyWaitOpen = 1.5f + rand.nextFloat() * 1f  // 1.5-2.5 сек
                println("[FNAF] TIMOFEY AT DOOR")
            }
        }
    }
}
