package com.example.memorygame.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

fun isPermissonGranted(context:Context, permisson: String) : Boolean{
    return ContextCompat.checkSelfPermission(context,permisson) == PackageManager.PERMISSION_GRANTED
}

fun requestPermission(activity : Activity? , permisson: String,requestCode : Int){
    ActivityCompat.requestPermissions(activity!!, arrayOf(permisson),requestCode)
}