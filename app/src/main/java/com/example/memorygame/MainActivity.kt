package com.example.memorygame

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.MemoGame
import com.example.memorygame.models.UserImageList
import com.example.memorygame.utils.EXTRA_BOARD_SIZE
import com.example.memorygame.utils.EXTRA_GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object{
        private const val CREATE_REQUEST_CODE = 111
        private const val MAIN = "MainActivity"
    }
    private val db = Firebase.firestore
    private var gameName : String? = null
    private var customGameImages  : List<String> ?= null
    private lateinit var memoryGame: MemoGame
    private var boardSize: BoardSize = BoardSize.EASY
    private lateinit var adapter: MemoryBoardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        setupBoard()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                //setup the game again
                if(memoryGame.getNumMoves()>0 && !memoryGame.haveWonGame()){
                    showAlertDialog("Quit your current game?", null,View.OnClickListener {
                        setupBoard()
                    })
                } else{
                    setupBoard()
                }
                return true
            }
            R.id.mi_newSize -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom ->{
                showCreationDialog()
                return true
            }
            R.id.mi_download ->{
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName == null){
                Log.e(MAIN,"Got null custom game from Create Activity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

     fun showDownloadDialog() {
        val boardDownloadView =LayoutInflater.from(this).inflate(R.layout.dialog_download_board,null)
        showAlertDialog("Fetch Memory Game",boardDownloadView,View.OnClickListener {
                // Grab the text of the game name that user wants to download
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }

     fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener {  document ->
            val userImageList =document.toObject(UserImageList::class.java)
            if(userImageList?.images == null){
                Log.e(MAIN, "Invalid Custom Game Data From Firestore")
                Snackbar.make(rl_root, "Sorry , we couldn't find your game, $customGameName",Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            val numCards = userImageList.images.size * 2
            boardSize = BoardSize.getByValue(numCards)
            customGameImages = userImageList.images
            for(imageUrl in userImageList.images){
                Picasso.get().load(imageUrl).fetch()
            }
            Snackbar.make(rl_root, "You're now Playing '$customGameName' ! ",Snackbar.LENGTH_SHORT).show()
            gameName = customGameName
            setupBoard()

        }.addOnFailureListener { Exception ->
            Log.e(MAIN,"Error occured with exception" ,Exception)
        }
    }

     fun showCreationDialog() {
        // Ask which size game they want to create
        val boardSizeView =LayoutInflater.from(this,).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board",boardSizeView,View.OnClickListener {
            // Set the new value for the board size
            val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rb_easy -> BoardSize.EASY
                R.id.rb_medium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            // Navigate user to new activity
            val intent   = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE,desiredBoardSize)
            startActivityForResult(intent,CREATE_REQUEST_CODE)
        })
    }

    fun showNewSizeDialog() {
        val boardSizeView =LayoutInflater.from(this,).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rb_easy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rb_medium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rb_hard)
        }
        showAlertDialog("Choose new size",boardSizeView,View.OnClickListener {
            // Set the new value for the board size
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rb_easy -> BoardSize.EASY
                R.id.rb_medium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setupBoard()
        })
    }

    private fun showAlertDialog(title:String, view:View? , positiveButtonClickListener : View.OnClickListener) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setNegativeButton("Cancel",null)
                .setPositiveButton("OK"){ _,_ ->
                    positiveButtonClickListener.onClick(null)

        }.show()
    }

    private fun setupBoard() {
        supportActionBar?.title =gameName ?: getString(R.string.app_name)
        when(boardSize){
            BoardSize.EASY -> {
                tv_numMoves.text = "Easy : 4 x 2"
                tv_numPairs.text = "Pairs : 0 / 4"
            }
            BoardSize.MEDIUM -> {
                tv_numMoves.text = "Easy : 6 x 3"
                tv_numPairs.text = "Pairs : 0 / 9"
            }
            BoardSize.HARD -> {
                tv_numMoves.text = "Easy : 6 x 4"
                tv_numPairs.text = "Pairs : 0 / 12"
            }
        }

        tv_numPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        memoryGame = MemoGame(boardSize,customGameImages)
        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, object : MemoryBoardAdapter.CardClickListener {
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }

        })
        rv_board.adapter = adapter
        rv_board.setHasFixedSize(true)
        rv_board.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    private fun updateGameWithFlip(position: Int) {

        //Error Handling
        if (memoryGame.haveWonGame()) {
            Snackbar.make(rl_root, "You already Won! ", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (memoryGame.isCardFaceUp(position)) {
            Snackbar.make(rl_root, "Invalid Move!", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (memoryGame.flipCard(position)) {
            Log.d("log1", "Found a Match ! Num Pairs Found : ${memoryGame.numPairsFound} ")
            val color = ArgbEvaluator().evaluate(memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                    ContextCompat.getColor(this, R.color.color_progress_none),
                    ContextCompat.getColor(this, R.color.color_progress_full)
            ) as Int
            tv_numPairs.setTextColor(color)
            tv_numPairs.text = "Pairs : ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if (memoryGame.haveWonGame()) {
                Snackbar.make(rl_root, "You Won! Congratulations", Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(rl_root, intArrayOf(Color.GREEN,Color.MAGENTA,Color.YELLOW)).oneShot()
            }
        }
        tv_numMoves.text = "Moves : ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}