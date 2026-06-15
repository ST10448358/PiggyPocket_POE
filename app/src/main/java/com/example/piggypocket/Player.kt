package com.example.piggypocket

import android.graphics.RectF

/**
 * Coupon Runner SA - Player Model
 */
class Player(
    var x: Float,
    var y: Float,
    val width: Float = 120f,
    val height: Float = 150f
) {
    var currentLane: Int = 1
    
    // Jump mechanics
    var isJumping = false
    var jumpVelocity = 0f
    private val gravity = 1.5f
    private val jumpStrength = -35f
    var groundY = 0f

    fun jump() {
        if (!isJumping) {
            isJumping = true
            jumpVelocity = jumpStrength
        }
    }

    fun update() {
        if (isJumping) {
            y += jumpVelocity
            jumpVelocity += gravity
            
            if (y >= groundY) {
                y = groundY
                isJumping = false
                jumpVelocity = 0f
            }
        }
    }

    fun getRect(): RectF {
        // Smaller hitbox for better gameplay feel
        return RectF(x - width/3, y - height, x + width/3, y)
    }
}
