package com.example.memorygame.utils

import android.graphics.Bitmap

object BitmapScaler{
    // To scale and maintain aspect ratio with desired width
    fun scaleToFitWidth(b : Bitmap, width : Int) : Bitmap {
        val factor = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b,width, (b.height * factor).toInt(),true)
    }
    // To scale and maintain aspect ratio with desired height
    fun scaleToFitHeight(b : Bitmap, height : Int) : Bitmap {
        val factor = height / b.height.toFloat()
        return Bitmap.createScaledBitmap(b,(b.width * factor).toInt(), height,true)
    }

}