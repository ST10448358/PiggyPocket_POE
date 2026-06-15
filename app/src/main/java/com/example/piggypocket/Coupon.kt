package com.example.piggypocket

import android.graphics.RectF

/**
 * Coupon Runner SA - Coupon Model
 */
class Coupon(
    var x: Float,
    var y: Float,
    val type: CouponType
) {
    private val size = 70f

    enum class CouponType(val storeName: String, val bonus: Int, val icon: String) {
        PICK_N_PAY("Pick n Pay", 20, "🛍️"),
        CHECKERS("Checkers", 0, "🏁"), // Double coins logic
        WOOLWORTHS("Woolworths", 0, "🛡️"), // Shield logic
        TAKEALOT("Takealot", 0, "🧲"), // Magnet logic
        MR_D("Mr D", 0, "🚀") // Speed boost logic
    }

    fun getRect(): RectF {
        return RectF(x - size, y - size, x + size, y + size)
    }
}
