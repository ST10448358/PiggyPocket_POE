package com.example.piggypocket

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import java.util.*

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var playing = false
    private val random = Random()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var player: Player? = null
    private val coins = mutableListOf<Coin>()
    private val obstacles = mutableListOf<Obstacle>()
    private val coupons = mutableListOf<Coupon>()
    
    private var score = 0
    private var level = 1
    private var laneWidth = 0f
    private var speed = 15f
    private var roadOffset = 0f // Offset for scrolling lane lines
    
    // Power-up states
    private var doubleCoinsActive = false
    private var magnetActive = false
    private var shieldActive = false
    private var speedBoostActive = false

    // Pre-allocate road line dash effect
    private val dashEffect = DashPathEffect(floatArrayOf(50f, 50f), 0f)
    
    var onUpdate: ((Int, Int) -> Unit)? = null // score, level
    var onGameOver: ((Int) -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            player?.jump()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val playerObj = player ?: return false
            val e1x = e1?.x ?: return false
            val diffX = e2.x - e1x
            if (Math.abs(diffX) > 100) {
                if (diffX > 0) {
                    if (playerObj.currentLane < 2) playerObj.currentLane++
                } else {
                    if (playerObj.currentLane > 0) playerObj.currentLane--
                }
            }
            return true
        }
    })

    fun startGame() {
        playing = true
        score = 0
        level = 1
        speed = 15f
        roadOffset = 0f
        coins.clear()
        obstacles.clear()
        coupons.clear()
        
        resetPowerUps()
        
        // Ensure width/height are ready
        val startY = if (height > 0) height - 300f else 1000f
        player = Player(width / 2f, startY)
        player?.groundY = startY
        player?.currentLane = 1
        
        invalidate()
    }

    private fun resetPowerUps() {
        doubleCoinsActive = false
        magnetActive = false
        shieldActive = false
        speedBoostActive = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Always calculate dimensions if available
        if (width > 0) laneWidth = width / 3f

        if (!playing) {
            // Draw a static "Ready" state if player exists
            player?.let { drawGame(canvas) }
            return
        }

        update()
        drawGame(canvas)
        
        // Force the next frame
        postInvalidateOnAnimation()
    }

    private fun update() {
        if (!playing) return
        
        val playerObj = player ?: return
        playerObj.update()
        
        // Smooth lane movement
        val targetX = (playerObj.currentLane * laneWidth) + (laneWidth / 2f)
        playerObj.x += (targetX - playerObj.x) * 0.2f

        // Scrolling road effect
        val currentSpeed = if (speedBoostActive) speed * 1.5f else speed
        roadOffset += currentSpeed
        if (roadOffset > 100f) roadOffset = 0f

        // Difficulty progression
        speed = 15f + (score / 100f)
        if (score > 500 && level == 1) level = 2
        if (score > 1200 && level == 2) level = 3

        // Spawn logic (Random but controlled)
        if (random.nextInt(100) < 3 + level) spawnCoin()
        if (random.nextInt(100) < 2 + level) spawnObstacle()
        if (random.nextInt(1000) < 2) spawnCoupon()

        // Update items
        updateCoins(playerObj)
        updateObstacles(playerObj)
        updateCoupons(playerObj)
    }

    private fun spawnCoin() {
        val lane = random.nextInt(3)
        val type = random.nextInt(10)
        val (value, label) = when {
            type < 6 -> Pair(1, "R1")
            type < 9 -> Pair(2, "R2")
            else -> Pair(5, "R5")
        }
        coins.add(Coin((lane * laneWidth) + (laneWidth / 2f), -100f, value, label))
    }

    private fun spawnObstacle() {
        val lane = random.nextInt(3)
        val type = Obstacle.ObstacleType.values()[random.nextInt(Obstacle.ObstacleType.values().size)]
        obstacles.add(Obstacle((lane * laneWidth) + (laneWidth / 2f), -100f, type))
    }

    private fun spawnCoupon() {
        val lane = random.nextInt(3)
        val type = Coupon.CouponType.values()[random.nextInt(Coupon.CouponType.values().size)]
        coupons.add(Coupon((lane * laneWidth) + (laneWidth / 2f), -100f, type))
    }

    private fun updateCoins(playerObj: Player) {
        val iterator = coins.iterator()
        while (iterator.hasNext()) {
            val coin = iterator.next()
            coin.y += if (speedBoostActive) speed * 1.5f else speed
            
            if (magnetActive && Math.abs(coin.y - playerObj.y) < 500) {
                coin.x += (playerObj.x - coin.x) * 0.1f
            }

            if (RectF.intersects(playerObj.getRect(), coin.getRect())) {
                score += if (doubleCoinsActive) coin.value * 2 else coin.value
                onUpdate?.invoke(score, level)
                iterator.remove()
                continue
            }
            if (coin.y > height) iterator.remove()
        }
    }

    private fun updateObstacles(playerObj: Player) {
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.y += if (speedBoostActive) speed * 1.5f else speed
            
            if (!playerObj.isJumping && RectF.intersects(playerObj.getRect(), obstacle.getRect())) {
                if (shieldActive) {
                    shieldActive = false
                } else {
                    playing = false
                    onGameOver?.invoke(score)
                }
                iterator.remove()
                continue
            }
            if (obstacle.y > height) iterator.remove()
        }
    }

    private fun updateCoupons(playerObj: Player) {
        val iterator = coupons.iterator()
        while (iterator.hasNext()) {
            val coupon = iterator.next()
            coupon.y += speed
            
            if (RectF.intersects(playerObj.getRect(), coupon.getRect())) {
                applyCoupon(coupon.type)
                iterator.remove()
                continue
            }
            if (coupon.y > height) iterator.remove()
        }
    }

    private fun applyCoupon(type: Coupon.CouponType) {
        when (type) {
            Coupon.CouponType.PICK_N_PAY -> score += 20
            Coupon.CouponType.CHECKERS -> activatePowerUp { doubleCoinsActive = true }
            Coupon.CouponType.WOOLWORTHS -> shieldActive = true
            Coupon.CouponType.TAKEALOT -> activatePowerUp { magnetActive = true }
            Coupon.CouponType.MR_D -> activatePowerUp { speedBoostActive = true }
        }
        onUpdate?.invoke(score, level)
    }

    private fun activatePowerUp(action: () -> Unit) {
        action()
        postDelayed({
            resetPowerUps()
        }, 10000) // 10 seconds duration
    }

    private fun drawGame(canvas: Canvas) {
        // 1. Draw Environment (Side grass)
        canvas.drawColor(Color.parseColor("#81C784"))
        
        // 2. Draw Road
        paint.color = Color.parseColor("#444444")
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // 3. Draw Lanes with Scrolling Animation
        paint.color = Color.WHITE
        paint.strokeWidth = 10f
        // Dynamic path effect for the scrolling illusion
        paint.pathEffect = DashPathEffect(floatArrayOf(50f, 50f), -roadOffset)
        canvas.drawLine(laneWidth, 0f, laneWidth, height.toFloat(), paint)
        canvas.drawLine(laneWidth * 2, 0f, laneWidth * 2, height.toFloat(), paint)
        paint.pathEffect = null

        // 4. Draw Player
        player?.let {
            paint.textSize = 100f
            paint.textAlign = Paint.Align.CENTER
            // Shadow
            paint.color = Color.parseColor("#44000000")
            canvas.drawCircle(it.x, it.y + 5f, 30f, paint)
            
            val icon = if (it.isJumping) "🏃‍♂️💨" else "🏃‍♂️"
            canvas.drawText(icon, it.x, it.y, paint)
            
            if (shieldActive) {
                paint.style = Paint.Style.STROKE
                paint.color = Color.CYAN
                paint.strokeWidth = 10f
                canvas.drawCircle(it.x, it.y - 75f, 100f, paint)
                paint.style = Paint.Style.FILL
            }
        }

        // 5. Draw Collectibles & Obstacles
        paint.textAlign = Paint.Align.CENTER
        for (coin in coins) {
            // Glow effect for coins
            paint.color = Color.parseColor("#33FFD600")
            canvas.drawCircle(coin.x, coin.y - 30f, 50f, paint)
            
            paint.textSize = 60f
            paint.color = Color.WHITE
            canvas.drawText(coin.label, coin.x, coin.y, paint)
        }
        
        for (obstacle in obstacles) {
            paint.textSize = 80f
            canvas.drawText(obstacle.type.icon, obstacle.x, obstacle.y, paint)
        }
        
        for (coupon in coupons) {
            // Glow effect for coupons
            paint.color = Color.parseColor("#44FFFFFF")
            canvas.drawCircle(coupon.x, coupon.y - 30f, 60f, paint)
            
            paint.textSize = 90f
            canvas.drawText(coupon.type.icon, coupon.x, coupon.y, paint)
            paint.textSize = spToPx(12f)
            paint.color = Color.WHITE
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(coupon.type.storeName, coupon.x, coupon.y + 40f, paint)
            paint.typeface = Typeface.DEFAULT
        }
    }

    private fun spToPx(sp: Float): Float {
        return sp * resources.displayMetrics.scaledDensity
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
