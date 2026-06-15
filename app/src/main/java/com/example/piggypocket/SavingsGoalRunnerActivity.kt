package com.example.piggypocket

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SavingsGoalRunnerActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var tvScore: TextView
    private lateinit var tvHighScore: TextView
    private lateinit var tvLevel: TextView
    private lateinit var cvOverlay: CardView
    private lateinit var tvOverlayTitle: TextView
    private lateinit var tvOverlayDesc: TextView
    private lateinit var btnAction: Button
    private lateinit var layoutHowToPlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings_goal_runner)

        gameView = findViewById(R.id.gameView)
        tvScore = findViewById(R.id.tvGameSavings)
        tvLevel = findViewById(R.id.tvGameLevel)
        
        cvOverlay = findViewById(R.id.cvOverlay)
        tvOverlayTitle = findViewById(R.id.tvOverlayTitle)
        tvOverlayDesc = findViewById(R.id.tvOverlayDesc)
        btnAction = findViewById(R.id.btnAction)
        layoutHowToPlay = findViewById(R.id.layoutHowToPlay)

        val prefs = getSharedPreferences("coupon_runner_prefs", Context.MODE_PRIVATE)
        val highScore = prefs.getInt("high_score", 0)

        tvScore.text = "Score: 0"
        tvLevel.text = "Township Streets"

        findViewById<ImageButton>(R.id.btnExitGame).setOnClickListener {
            finish()
        }

        gameView.onUpdate = { score, level ->
            runOnUiThread {
                tvScore.text = "Score: $score"
                val levelName = when (level) {
                    1 -> "Township Streets"
                    2 -> "Shopping Mall"
                    3 -> "City Center"
                    4 -> "Highway"
                    else -> "Online Shopping World"
                }
                tvLevel.text = levelName
            }
        }

        gameView.onGameOver = { finalScore ->
            runOnUiThread {
                val currentHighScore = prefs.getInt("high_score", 0)
                if (finalScore > currentHighScore) {
                    prefs.edit().putInt("high_score", finalScore).apply()
                }
                showGameOverScreen(finalScore)
            }
        }

        btnAction.setOnClickListener {
            cvOverlay.visibility = View.GONE
            gameView.startGame()
        }
        
        // Initial Overlay Setup
        tvOverlayTitle.text = "Coupon Runner SA"
        tvOverlayDesc.text = "Collect South African brand coupons and coins!\nHigh Score: $highScore"
        layoutHowToPlay.visibility = View.VISIBLE
        btnAction.text = "START RUNNING"
    }

    private fun showGameOverScreen(score: Int) {
        tvOverlayTitle.text = "Game Over!"
        tvOverlayDesc.text = "You collected R$score worth of savings.\nTry again to beat your high score!"
        layoutHowToPlay.visibility = View.GONE
        btnAction.text = "RETRY"
        cvOverlay.visibility = View.VISIBLE
    }
}
