package com.example.memorygame

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_create.*
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object{
        private const val PICK_PHOTO_CODE = 655
        private const val READ_EXTERNAL_PHOTO_CODE = 248
        private const val READ_PHOTO_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val TAG = "CreateActivity"
        private const val MIN_GAME_LENGTH = 3
        private const val MAX_GAME_LENGTH = 14
    }

    private lateinit var rvImagePicker : RecyclerView
    private lateinit var btnSave : Button
    private lateinit var adapter : ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private lateinit var etGameName : EditText
    private lateinit var pbUploading :ProgressBar

    private  var numImagesRequired = -1
    private var chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.et_gameName)
        btnSave = findViewById(R.id.btn_save)
        pbUploading = findViewById(R.id.pbUploading)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize =  intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose Pics (0 / ${numImagesRequired})"

        btnSave.setOnClickListener {
            saveDataToFireBase()
        }

        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_LENGTH))
        etGameName.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                btnSave.isEnabled = shouldEnableSaveButton()
            }

        })

        adapter = ImagePickerAdapter(this,chosenImageUris,boardSize,object : ImagePickerAdapter.ImageClickListener{
            override fun onPlaceHolderClicked() {
                if(isPermissonGranted(this@CreateActivity, READ_PHOTO_PERMISSION)){
                    launchIntentForPhotos()
                } else{
                    requestPermission(this@CreateActivity, READ_PHOTO_PERMISSION, READ_EXTERNAL_PHOTO_CODE)
                }

            }

        })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this,boardSize.getWidth())
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == READ_EXTERNAL_PHOTO_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                launchIntentForPhotos()
            }else
            {
                Toast.makeText(this,"Please Provide Permission for accessing external photos",Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null)
        {
            Log.w(TAG, " Did not get data back from the launched activity, user likely canceled flow")
            return
        }
        val selectedUri = data.data
        val clipData = data.clipData
        if(clipData!=null){
            Log.i(TAG, "Clip Data num Images ${clipData.itemCount} : $clipData")
            for(i in 0 until clipData.itemCount){
                val clipItem = clipData.getItemAt(i)
                if(chosenImageUris.size < numImagesRequired){
                    chosenImageUris.add(clipItem.uri)
                }
            }
        }else if(selectedUri!=null){
            Log.i(TAG, "data : $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose Pics (${chosenImageUris.size}/$numImagesRequired)"
        btnSave.isEnabled = shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {
        if(chosenImageUris.size != numImagesRequired){
            return false
        }
        if(etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_LENGTH){
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        startActivityForResult(Intent.createChooser(intent, "Choose Pics"),PICK_PHOTO_CODE)
    }

    private fun saveDataToFireBase() {
        btnSave.isEnabled = false
        val customGameName = etGameName.text.toString()

        // Check that we're not over writing someone else's data by saving the same game name
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if(document!=null && document.data!=null) {
                AlertDialog.Builder(this)
                    .setTitle("The name $customGameName is already in use")
                    .setPositiveButton("OK",null)
                    .show()
                btnSave.isEnabled = true
            }else{
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener {exception ->
            Log.e(TAG, "Encounter error while saving the game",exception)
            Toast.makeText(this,"Encountered error while saving the game",Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }

    }

    private fun handleImageUploading(gameName: String) {
        pbUploading.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrl = mutableListOf<String>()
        for((index,photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray).continueWithTask { photoUploadTask ->
                Log.i(TAG, "Uploaded bytes : ${photoUploadTask.result?.bytesTransferred}")
                photoReference.downloadUrl
            }.addOnCompleteListener { downloadUrlTask->
                if(!downloadUrlTask.isSuccessful){
                    Log.i(TAG, "Exception with firebase storage", downloadUrlTask.exception)
                    Toast.makeText(this, "Failed to upload Image",Toast.LENGTH_SHORT).show()
                    didEncounterError = true
                    return@addOnCompleteListener
                }
                if(didEncounterError==true)
                {
                    pbUploading.visibility = View.GONE
                    return@addOnCompleteListener
                }
                val downloadUrl = downloadUrlTask.result.toString()
                uploadedImageUrl.add(downloadUrl)
                pbUploading.progress = uploadedImageUrl.size * 100/chosenImageUris.size
                Log.i(TAG,"Finishing uploading $photoUri, num uploaded ${uploadedImageUrl.size}")
                if(uploadedImageUrl.size == chosenImageUris.size){
                    handleAllImagesUploaded(gameName,uploadedImageUrl)
                }
            }
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
          db.collection("games").document(gameName)
              .set(mapOf("images" to imageUrls))
              .addOnCompleteListener { gameCreationTask ->
                  pbUploading.visibility = View.GONE
                    if(!gameCreationTask.isSuccessful){
                        Log.e(TAG,"Exception with game creation" , gameCreationTask.exception)
                        Toast.makeText(this, "Failed to create the game",Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }
                  Log.i(TAG, "Successfully created game $gameName")
                  AlertDialog.Builder(this)
                      .setTitle("Upload Complete! Lets Play Your Game $gameName")
                      .setPositiveButton("OK"){ _,_ ->
                          val resultData = Intent()
                          resultData.putExtra(EXTRA_GAME_NAME,gameName)
                          setResult(Activity.RESULT_OK,resultData)
                          finish()
                      }.show()
              }

    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source = ImageDecoder.createSource(contentResolver,photoUri)
            ImageDecoder.decodeBitmap(source)
        }else{
            MediaStore.Images.Media.getBitmap(contentResolver,photoUri)
        }
        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap,250)
        Log.i(TAG, " Scaled Bitmap ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG,60,byteOutputStream)
        return byteOutputStream.toByteArray()
    }

}