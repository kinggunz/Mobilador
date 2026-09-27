package com.mobibawah.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.View
import kotlin.random.Random

/**
 * Mini-game "Lompat Kaktus" ala endless-runner: karakter lari otomatis
 * ke kanan (kaktus datang dari kanan ke kiri, memberi ilusi berlari),
 * tekan tombol yang sudah dipilih untuk lompat, kena kaktus = mati.
 *
 * Ini BUKAN cuma buat hiburan — tujuannya sebagai BUKTI NYATA bahwa
 * tombol keyboard fisik yang kamu pilih benar-benar terbaca oleh Android
 * di HP-mu: kalau lompatnya responsif setiap kamu pencet tombolnya,
 * berarti jalur pembacaan fisik (yang sama persis dipakai untuk mapping
 * sungguhan di game) sudah pasti bekerja dengan benar.
 */
class RunnerGameView(context: Context) : View(context) {

    var onGameOver: ((score: Int) -> Unit)? = null
    var onScoreChanged: ((score: Int) -> Unit)? = null

    private val paintGround = Paint().apply { color = Color.parseColor("#444444"); strokeWidth = 4f }
    private val paintPlayer = Paint().apply { color = Color.parseColor("#2196F3") }
    private val paintCactus = Paint().apply { color = Color.parseColor("#4CAF50") }
    private val paintText = Paint().apply { color = Color.WHITE; textSize = 42f; isAntiAlias = true }
    private val paintGameOver = Paint().apply {
        color = Color.parseColor("#E57373"); textSize = 56f; isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val paintHint = Paint().apply {
        color = Color.parseColor("#AAAAAA"); textSize = 30f; isAntiAlias = true; textAlign = Paint.Align.CENTER
    }

    private val handler = Handler(Looper.getMainLooper())
    private var loopRunnable: Runnable? = null

    private var groundY = 0f
    private val playerX = 120f
    private var playerY = 0f
    private val playerSize = 64f
    private var velocityY = 0f
    private val gravity = 2600f
    private val jumpVelocity = -1050f
    private var isJumping = false

    private var speed = 480f
    private val obstacles = mutableListOf<RectF>()
    private var distanceSinceSpawn = 0f
    private var nextSpawnGap = 500f
    private var score = 0
    private var running = false
    private var gameOver = false
    private var everStarted = false
    private var lastFrameTime = 0L

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        groundY = h * 0.75f
        playerY = groundY - playerSize
        invalidate()
    }

    /** Dipanggil setiap kali tombol tes ditekan: lompat kalau sedang lari, mulai/ulang kalau belum/mati. */
    fun onTestKeyPressed() {
        if (!running) {
            start()
            return
        }
        if (!isJumping) {
            isJumping = true
            velocityY = jumpVelocity
        }
    }

    fun start() {
        obstacles.clear()
        score = 0
        speed = 480f
        playerY = groundY - playerSize
        velocityY = 0f
        isJumping = false
        gameOver = false
        running = true
        everStarted = true
        distanceSinceSpawn = 0f
        nextSpawnGap = Random.nextInt(450, 750).toFloat()
        lastFrameTime = System.currentTimeMillis()
        onScoreChanged?.invoke(score)

        loopRunnable?.let { handler.removeCallbacks(it) }
        val runnable = object : Runnable {
            override fun run() {
                updateGame()
                invalidate()
                if (running) handler.postDelayed(this, 16L)
            }
        }
        loopRunnable = runnable
        handler.post(runnable)
    }

    fun stopLoop() {
        running = false
        loopRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun updateGame() {
        val now = System.currentTimeMillis()
        val dt = ((now - lastFrameTime).coerceIn(0, 50)) / 1000f
        lastFrameTime = now
        if (gameOver) return

        if (isJumping) {
            velocityY += gravity * dt
            playerY += velocityY * dt
            if (playerY >= groundY - playerSize) {
                playerY = groundY - playerSize
                isJumping = false
                velocityY = 0f
            }
        }

        val moveAmount = speed * dt
        distanceSinceSpawn += moveAmount
        if (distanceSinceSpawn > nextSpawnGap && width > 0) {
            distanceSinceSpawn = 0f
            nextSpawnGap = Random.nextInt(450, 750).toFloat()
            val w = width.toFloat()
            val cactusW = 42f
            val cactusH = 72f
            obstacles.add(RectF(w, groundY - cactusH, w + cactusW, groundY))
        }

        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val rect = iterator.next()
            rect.left -= moveAmount
            rect.right -= moveAmount
            if (rect.right < 0) {
                iterator.remove()
                score += 1
                onScoreChanged?.invoke(score)
            }
        }

        speed += 6f * dt // makin lama makin cepat, biar makin menantang

        val playerRect = RectF(playerX, playerY, playerX + playerSize, playerY + playerSize)
        for (rect in obstacles) {
            if (RectF.intersects(playerRect, rect)) {
                gameOver = true
                running = false
                onGameOver?.invoke(score)
                break
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#0D0D0D"))
        canvas.drawLine(0f, groundY, width.toFloat(), groundY, paintGround)
        canvas.drawRect(playerX, playerY, playerX + playerSize, playerY + playerSize, paintPlayer)
        for (rect in obstacles) canvas.drawRect(rect, paintCactus)
        canvas.drawText("Skor: $score", 24f, 56f, paintText)

        if (!everStarted) {
            canvas.drawText("Tekan tombol tes untuk mulai lari", width / 2f, height / 2f, paintHint)
        } else if (gameOver) {
            canvas.drawText("MATI! Tekan tombol tes lagi untuk ulang", width / 2f, height / 2f, paintGameOver)
        }
    }
}
