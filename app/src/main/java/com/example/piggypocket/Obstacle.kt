package com.example.piggypocket

import android.graphics.RectF

/**
 * Coupon Runner SA - Obstacle Model
 */
class Obstacle(
    var x: Float,
    var y: Float,
    val type: ObstacleType
) {
    private val width = 100f
    private val height = 100f

    enum class ObstacleType(val icon: String) {
        CART("🛒"),
        CONE("⚠️"),
        POTHOLE("🕳️"),
        BOX("📦"),
        TROLLEY("🛒")
    }

    fun getRect(): RectF {
        return RectF(x - width/2, y - height, x + width/2, y)
    }
}
