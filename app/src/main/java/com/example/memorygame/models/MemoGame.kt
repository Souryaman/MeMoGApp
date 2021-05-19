package com.example.memorygame.models

import com.example.memorygame.utils.DEFAULT_ICONS

class MemoGame (private val boardSize: BoardSize, customImages : List<String>?){


    val cards : List<MemoryCard>
    var numPairsFound = 0
    private var numCardFlips =0
    private var indexOfSingleSelectedCards  : Int? = null
    init {
        if(customImages == null) {
            val choosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
            val randamizedImages = (choosenImages + choosenImages).shuffled()
            cards = randamizedImages.map { MemoryCard(it) }
        }else{
            val randamizedImages =  (customImages + customImages).shuffled()
            cards = randamizedImages.map { MemoryCard(it.hashCode(),it) }
        }
    }

    fun flipCard(position: Int) : Boolean{
        numCardFlips++
        val card = cards[position]
        //Three cases
        // 0 cards previously flipped over -> flip over the selected card
        // 1 card previously flipped over -> flip over the selected card + check if images match
        // 2 cards previously flipped over -> restore cards + flip over the selected card

        var foundMatch = false

        if(indexOfSingleSelectedCards == null) {
            // 0 or 2 cards previously flipped over
            restoreCards()
            indexOfSingleSelectedCards = position
        }
        else{
            // 1 card previously flipped over
            foundMatch = checkForMatch(indexOfSingleSelectedCards!!,position)
            indexOfSingleSelectedCards = null
        }

        card.isFaceUp = !card.isFaceUp

        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int) : Boolean{
        if( cards[position1].identifier != cards[position2].identifier)
        {
            return false
        }
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        for ( card in cards){
            if(!card.isMatched){
                card.isFaceUp = false
            }

        }
    }

    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        return numCardFlips / 2
    }
}