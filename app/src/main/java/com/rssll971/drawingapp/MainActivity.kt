package com.rssll971.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileSystem
import java.util.ArrayList
import kotlin.Exception
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity() {

    //TODO FUNCTION FOR GETTING IMAGE FROM CAMERA
    //TODO COLOR PALETTE
    //TODO DRAG VIEW
    //TODO HORIZONTAL MODE
    //TODO ADS
    //TODO CLARIFY DRAWABLE VALUES COLORS THEME
    //TODO REDESIGN OF DIALOGS

    //GET ALL PERMISSIONS
    private fun requestStoragePermissions(){
        //Check if it worth to show permissions request
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                PERMISSIONS_REQUIRED.toString())){


        }
        //request permissions
        else{
            ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, PERMISSIONS_ALL_CODE)
        }

    }

    //actions on user's permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_ALL_CODE){
            var allGranted: Boolean = false

            //check if all permissions are granted
            for (i in grantResults.indices){
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    allGranted = true
                }
                else{
                    //Nothing have to do
                }
            }
        }
    }
    //Check that all permissions are allowed
    private fun isPermissionsAreAllowed(): Boolean{
        var result: Boolean = false
        if (
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                ){result = true}
        else{
            //Nothing to do
        }
        return result
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == GALLERY_CODE){
                //get user image as background, using exception
                try {
                    //check for available data
                    if (data!!.data != null){
                        //make background visible
                        iv_users_image.visibility = View.VISIBLE
                        //set image
                        iv_users_image.setImageURI(data.data)
                    }
                    //if something goes wrong
                    else{
                        Toast.makeText(this, "Wrong data type",
                            Toast.LENGTH_LONG).show()
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
        else{ }
    }

    //Function which give us final bitmap for sharing
    private fun getBitmapFromView(view: View): Bitmap{
        //save our bitmap in object with current width, height and bitmap type
        val returnedBitmap = Bitmap.createBitmap(
                view.width, view.height, Bitmap.Config.ARGB_8888)
        //get canvas from returned object
        val canvas = Canvas(returnedBitmap)

        //make sandwich with our background image and canvases
        val backgroundDrawable = view.background
        if (backgroundDrawable != null){
            backgroundDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        //return all
        return returnedBitmap
    }

    //Async task function for sharing final image
    private inner class BitmapAsyncTask(val myBitmap: Bitmap) : AsyncTask<Any, Void, String>(){
        //Loading dialog block
        var loadingDialog = Dialog(this@MainActivity)
        //show
        fun showProgressDialog(){
            loadingDialog.setContentView(R.layout.dialog_progress)
            loadingDialog.show()
        }
        //dismiss
        fun cancelProgressDialog(){
            loadingDialog.dismiss()
        }


        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if (myBitmap != null){
                try {
                    //variable where we will save our output data
                    val bytes = ByteArrayOutputStream()
                    //compress our bitmap to PNG using stream of val bytes
                    myBitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                    //make it as single file
                                //external directory -> as absolute file -> separate ->
                    val myFile = File(//externalCacheDir!!.absoluteFile.toString() +
                            "/storage/emulated/0/Download" + File.separator + System.currentTimeMillis()/1000 + ".png")
                    //stream of our file
                    val myFileOS = FileOutputStream(myFile)
                    //start writing
                    myFileOS.write(bytes.toByteArray())
                    //close os write operation
                    myFileOS.close()
                    //store the result to path
                    result = myFile.absolutePath
                }
                catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }

            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            if (result != null){
                Toast.makeText(this@MainActivity,
                        "File saved: $result", Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(this@MainActivity,
                        "Something went wrong \nPlease try again", Toast.LENGTH_LONG).show()
            }
            //share image for another app
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){
                path, uri -> val sharingIntent = Intent()
                sharingIntent.action = Intent.ACTION_SEND
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
                sharingIntent.type = "image/png"
                startActivity(
                        Intent.createChooser( sharingIntent, "Share")
                )
            }
        }


    }



    //Permissions variables
    // The request code used in ActivityCompat.requestPermissions()
    // and returned in the Activity's onRequestPermissionsResult()
    companion object{
        private const val PERMISSIONS_ALL_CODE: Int = 103
        private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        private const val GALLERY_CODE = 104
    }

    //variable to store statically text of brush size
    private var textBrushSize: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //Make brush size by default to 1
        drawing_view.setBrushSize(1.toFloat())

        //*************************************
        //ALL BUTTONS LISTENERS
        //thickness of brush on button
        btn_brush.setOnClickListener { view ->
            showBrushSizeDialog()
        }

        //undo button
        btn_undo.setOnClickListener {
            drawing_view.removeLastLine()
        }

        //trash button
        btn_trash.setOnClickListener {
            eraseAll()
        }

        //gallery button
        btn_gallery.setOnClickListener{
            //check for having permission
            if (isPermissionsAreAllowed()){
                //pick image from gallery
                //create new intent
                val pickImageIntent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickImageIntent, GALLERY_CODE)
            }
            //if don't have permission
            else{
                requestStoragePermissions()
            }
        }

        //button share
        btn_share.setOnClickListener {
            if (isPermissionsAreAllowed()){
                BitmapAsyncTask(getBitmapFromView(fl_image_container)).execute()
            }
            else{
                requestStoragePermissions()
            }
        }


        btn_camera.setOnClickListener {


        }

    }



    //DIALOG OF BRUSH SIZE FUNCTION
    private fun showBrushSizeDialog(){
        //create dialog window
        val brushDialog = Dialog(this)
        //initialize dialog layout
        brushDialog.setContentView(R.layout.dialog_brush_size)
        //make background color to transparent. it needs for round corners
        brushDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        brushDialog.setCanceledOnTouchOutside(false)
        //get values from brush dialog to local variables
            //brush size - next changing
        val brushSize = brushDialog.sb_brush_size
            //get current brush size to seek bar
        val currentBrushSize: Int = drawing_view.getBrushSize().toInt()
        brushSize.progress = 1
        brushSize.progress = textBrushSize
        //brushSize.progress = currentBrushSize
            //text option of brush size
        val displayBrushSize = brushDialog.tv_brush_size_display
            //button of confirmation
        val confirmationBrushSize = brushDialog.btn_confirm_size
        //upload brush size
        displayBrushSize.text = "Size: $textBrushSize"
        //display dialog menu
        brushDialog.show()
        //show current value of size
        brushSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                textBrushSize = i
                displayBrushSize.text = "Size: $textBrushSize"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        //change brush size
        confirmationBrushSize.setOnClickListener {
            //change brush size
            drawing_view.setBrushSize(brushSize.progress.toFloat())
            //turn off brush dialog
            brushDialog.dismiss()
        }
    }




    //CHANGE COLOR OF LINE USING BUTTONS
    //initialize directly in xml file
    //tag takes information of color
    fun colorClicked(view: View){
        //import button
        val button = view as Button
        //import color using tag of button
        val colorTag = button.tag.toString()
        drawing_view.setColor(colorTag)
    }


    //Erase all function
    fun eraseAll(){
        //erase background image
        iv_users_image.setImageResource(0)
        //erase lines
        drawing_view.removeAllLines()
    }




}