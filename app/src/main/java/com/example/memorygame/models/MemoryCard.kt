package com.example.memorygame.models

import android.content.ContentProvider

data class MemoryCard(
        val identifier : Int,
        val imageUrl : String ?= null,
        var isFaceUp : Boolean = false,
        var isMatched : Boolean = false
)