package com.example.piggypocket

import android.graphics.RectF

/**
 * Coupon Runner SA - Coin Model
 */
class Coin(
    var x: Float,
    var y: Float,
    val value: Int, // 1, 2, or 5
    val label: String // "R1", "R2", "R5"
) {
    private val size = 60f

    fun getRect(): RectF {
        return RectF(x - size, y - size, x + size, y + size)
    }
}
