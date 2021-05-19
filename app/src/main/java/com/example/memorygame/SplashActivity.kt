
package com.example.memorygame

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val topAnimation = AnimationUtils.loadAnimation(this,R.anim.top_animation)
        val middleAnimation = AnimationUtils.loadAnimation(this,R.anim.middle_animation)
        val bottomAnimation = AnimationUtils.loadAnimation(this,R.anim.bottom_animation)

        top_text_view.startAnimation(topAnimation)
        middle_text_view.startAnimation(middleAnimation)
        bottom_text_view.startAnimation(bottomAnimation)

        @Suppress("DEPRECATION")
        Handler().postDelayed(
                {

                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                },
                4000
        )
    }
}