package com.example.memorygame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_menu.*

class MenuActivity : AppCompatActivity() {
    private lateinit var mainActivity: MainActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        btn_pickSize.setOnClickListener {
            mainActivity.showNewSizeDialog()
        }
        btn_createCustom.setOnClickListener {
            mainActivity.showCreationDialog()
        }
        btn_downloadGame.setOnClickListener {
            mainActivity.showDownloadDialog()
        }
    }
}