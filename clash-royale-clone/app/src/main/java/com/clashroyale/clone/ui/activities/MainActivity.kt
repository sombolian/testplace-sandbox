package com.clashroyale.clone.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.clashroyale.clone.R
import com.clashroyale.clone.data.PlayerData

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val titleText = findViewById<TextView>(R.id.titleText)
        val subtitleText = findViewById<TextView>(R.id.subtitleText)
        val loadingBar = findViewById<ProgressBar>(R.id.loadingBar)
        val tapText = findViewById<TextView>(R.id.tapToStartText)

        // Animate title
        val scaleAnim = ScaleAnimation(
            0.5f, 1f, 0.5f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply { duration = 800; fillAfter = true }

        val alphaAnim = AlphaAnimation(0f, 1f).apply { duration = 800 }

        val animSet = AnimationSet(true)
        animSet.addAnimation(scaleAnim)
        animSet.addAnimation(alphaAnim)
        titleText.startAnimation(animSet)

        // Loading simulation
        subtitleText.alpha = 0f
        subtitleText.animate().alpha(1f).setStartDelay(500).setDuration(500).start()

        // Initialize player data on background thread
        Thread {
            PlayerData.getPlayer(applicationContext)

            runOnUiThread {
                loadingBar.visibility = View.GONE
                tapText.visibility = View.VISIBLE
                tapText.alpha = 0f
                tapText.animate().alpha(1f).setDuration(500).start()

                // Pulse animation for tap text
                val pulse = AlphaAnimation(1f, 0.3f).apply {
                    duration = 1000
                    repeatCount = AlphaAnimation.INFINITE
                    repeatMode = AlphaAnimation.REVERSE
                }
                tapText.startAnimation(pulse)
            }
        }.start()

        // Tap anywhere to continue
        findViewById<View>(R.id.splashRoot).setOnClickListener {
            if (tapText.visibility == View.VISIBLE) {
                startActivity(Intent(this, HomeActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }
    }
}
