package com.rssll971.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dinuscxj.gesture.MultiTouchGestureDetector
import com.google.android.gms.ads.*
import com.rssll971.drawingapp.databinding.ActivityMainBinding
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.Exception

class MainActivity : AppCompatActivity() {
    /**
     * Permission vars and list of needed permission for app
     *
     * Addition info:
     * The request code used in ActivityCompat.requestPermissions()
     * and returned in the Activity's onRequestPermissionsResult()
     */
    companion object{
        private const val PERMISSIONS_ALL_CODE: Int = 103
        private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private const val GALLERY_CODE = 104
    }
    /**
     * Binding
     */
    private lateinit var binding: ActivityMainBinding
    /**
     * Other vars
     */
    //variable to store statically text of brush size
    private var textBrushSize: Int = 1
    //var to make scaling of layout
    private var scaleFactor: Float = 1.0f
    //show extra menu or not
    private var isMenuShown: Boolean = false
    //portrait orientation active
    private var isPortraitMode: Boolean = true


    //Scale detector. Declare in onCreate, called by button
    private lateinit var myMultiTouchGestureDetector: MultiTouchGestureDetector
    //ADS
    //interstitial ad
    private lateinit var myInterstitialAd: InterstitialAd
    //banner ad
    private lateinit var myBannerAdView: AdView
    //id of interstitial ad
    private val adInterstitialID: String = "ca-app-pub-4362142146545991/4879230890"

    /**
     * Next two fun responsible for fullscreen mode and transparent navigation and status bars
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
    private fun hideSystemUI() {
        /**
         * Enables regular immersive mode.
         * For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
         * Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
         */
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }


    /** ACTIVITY STARTS**/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val  view = binding.root
        setContentView(view)

        //change orientation state
        when(requestedOrientation){
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {isPortraitMode = true
                Toast.makeText(this, "Portrait Mode", Toast.LENGTH_LONG).show()}
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {isPortraitMode = false
                Toast.makeText(this, "Landscape Mode", Toast.LENGTH_LONG).show()}
        }
        //Lock auto-screen orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        hideExtraMenu()

        //crutch when systemUI doesn't disappear
        Handler().postDelayed({onWindowFocusChanged(true)}, 1000)


        //Declare multi-touch detector
        myMultiTouchGestureDetector = MultiTouchGestureDetector(
                this, MultiTouchGestureDetectorListener())


        //Prepare and build Ads
        MobileAds.initialize(this)
        //get to local var
        myInterstitialAd = InterstitialAd(this)
        myInterstitialAd.adUnitId = adInterstitialID
        myBannerAdView = findViewById(R.id.adView_smart_banner)
        //load
        myInterstitialAd.loadAd(AdRequest.Builder().build())
        myBannerAdView.loadAd(AdRequest.Builder().build())

        //reload ads
        myInterstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                myInterstitialAd.loadAd(AdRequest.Builder().build())
            }
        }
        myBannerAdView.adListener = object : AdListener(){
            override fun onAdClosed() {
                myBannerAdView.loadAd(AdRequest.Builder().build())
            }
        }

        /**
         * Next lines every time set brush size to 1 on first app start
         * and
         * hide extra menu
         */
        binding.drawingView.setBrushSize(1.toFloat())



        /**
         * BLOCK of all listeners for any clickable objects
         */
        //move and draw layout
        //default scale
        binding.btnFit.setOnClickListener {
            defaultScaleAndPosition()
        }
        //active draw functionality
        binding.btnActiveDraw.setOnClickListener {
            //buttons background color changes
            binding.btnActiveDraw.setBackgroundResource(R.drawable.ib_grey_handling)
            binding.btnTransfer.setBackgroundResource(R.drawable.ib_white_handling)
            binding.drawingView.setIsTouchAllowed(true)
        }
        //drag and scale
        binding.btnTransfer.setOnClickListener {
            /**
             * Next statements responsible for scaling and dragging.
             * Firstly disable drag layout
             */
            binding.drawingView.setIsTouchAllowed(false)
            //buttons background color changes
            binding.btnActiveDraw.setBackgroundResource(R.drawable.ib_white_handling)
            binding.btnTransfer.setBackgroundResource(R.drawable.ib_grey_handling)
            //
            binding.flImageContainer.setOnTouchListener { v, event ->
                myMultiTouchGestureDetector.onTouchEvent(event)
            }
        }

        //color palette
        binding.btnPalette.setOnClickListener {
            showColorPickerDialog()
        }


        //secondary layout
        //brush
        binding.btnBrush.setOnClickListener {
            showBrushSizeDialog()
        }
        //undo
        binding.btnUndo.setOnClickListener {
            binding.drawingView.removeLastLine()
        }



        //extra layout
        //extra menu
        binding.btnExtraMenu.setOnClickListener {
            if (isMenuShown){
                hideExtraMenu()
            }
            else{
                //show menu and ads
                myInterstitialAd.show()
                showExtraMenu()
            }
        }
        //screen orientation
        binding.btnRotate.setOnClickListener {
            if (isPortraitMode){
                //switch to landscape
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            else{
                //switch to portrait
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        //trash
        binding.btnTrash.setOnClickListener {
            eraseAll()
            //default scale
            defaultScaleAndPosition()

            hideExtraMenu()
        }
        // share/save
        binding.btnShare.setOnClickListener {
            //default scale
            defaultScaleAndPosition()

            hideExtraMenu()

            if (isPermissionsAreAllowed()){
                //start saving new image
                BitmapAsyncTask(getBitmapFromView(binding.flImageContainer)).execute()
            }
            else{
                //request permissions again
                requestPermissions()
            }
        }
        //gallery
        binding.btnGallery.setOnClickListener{
            //default scale
            defaultScaleAndPosition()

            hideExtraMenu()

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
                requestPermissions()
            }
        }
        //info
        binding.btnInfo.setOnClickListener {
            hideExtraMenu()
            showInfo()
        }
        /** BLOCK ENDS**/

    }

    /**
     * Next inner class implement multi-touch functionality
     *  Takes opportunity to scale and drag frame layout
     */
    private inner class MultiTouchGestureDetectorListener :
        MultiTouchGestureDetector.SimpleOnMultiTouchGestureListener() {
        //scale frame fun
        override fun onScale(detector: MultiTouchGestureDetector?) {
            super.onScale(detector)
            scaleFactor *= detector?.scale ?: 1.0f
            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 5.0f))
            binding.flImageContainer.scaleX = scaleFactor
            binding.flImageContainer.scaleY = scaleFactor
        }
        //drag frame fun
        override fun onMove(detector: MultiTouchGestureDetector?) {
            super.onMove(detector)
            binding.flImageContainer.x += detector?.moveX ?: 0.0f
            binding.flImageContainer.y += detector?.moveY ?: 0.0f
        }
    }
    /**
     * Next fun fit frame to default
     */
    private fun defaultScaleAndPosition(){
        //default size
        scaleFactor = 1.0f
        binding.flImageContainer.scaleX = scaleFactor
        binding.flImageContainer.scaleY = scaleFactor
        //for portrait mode
        if (isPortraitMode) {
            //align relative to adView
            binding.flImageContainer.x = myBannerAdView.left.toFloat()
            binding.flImageContainer.y = myBannerAdView.bottom.toFloat()
        }
        else{
            binding.flImageContainer.x = 0.0f
            binding.flImageContainer.y = 0.0f
        }
    }

    /**
     * BLOCK responsible for all operations related with permissions
     **/
    /**
     * Next fun get all needed permissions for app,
     * such as Storage writing/reading
     * */
    private fun requestPermissions(){
        //Next if/else statement check necessity to show permissions request
        //permissions allowed
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                        PERMISSIONS_REQUIRED.toString())){
        }
        //request permissions
        else{
            ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, PERMISSIONS_ALL_CODE)
        }
    }
    /**
     * Next fun change local permission state var,
     * depending on result of permission request
     */
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_ALL_CODE){
            var allGranted: Boolean = false
            //check if all permissions are granted
            for (i in grantResults.indices){
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    allGranted = true
                }

            }

            if (!allGranted){
                Toast.makeText(this,
                        "Access to the storage is not available.\n\nPlease grant permission",
                        Toast.LENGTH_LONG).show()
            }

        }
    }
    /**
     * Next fun check permissions availability for app
     * */
    private fun isPermissionsAreAllowed(): Boolean{
        var result: Boolean = false
        if (
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ){result = true}


        return result
    }
    /** BLOCK ENDS**/


    /**
     * BLOCK Responsible for operations of:
     *      -image extracting from gallery
     *      -further implementing in app
     *      -exporting
     */
    /**
     * Next fun extracts user image from gallery and substitutes data in image view
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == GALLERY_CODE){
                //get user image as background, using exception
                try {
                    //check for available data
                    if (data!!.data != null){
                        //make background visible
                        binding.ivUsersImage.visibility = View.VISIBLE
                        //set image
                        binding.ivUsersImage.setImageURI(data.data)
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
    }
    /**
     * Next fun preparing our bitmap for next compressing with image
     * and
     * further sharing already final image
     */
    private fun getBitmapFromView(view: View): Bitmap{
        //save our bitmap in object with current width, height and bitmap type
        val returnedBitmap = Bitmap.createBitmap(
                view.width, view.height, Bitmap.Config.ARGB_8888)
        //get canvas from local bitmap
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
    /**
     * Next Class implements AsyncTask and responsible for all process of image exporting/sharing
     */
    private inner class BitmapAsyncTask(val myBitmap: Bitmap) : AsyncTask<Any, Void, String>(){
        //Loading simple dialog
        var loadingDialog = Dialog(this@MainActivity)
        //show progress, while image is exporting
        fun showProgressDialog(){
            loadingDialog.setContentView(R.layout.dialog_background_progress)
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
        /**
         * While progress dialog is showing
         * Next fun saves our prepared image in Folder DOWNLOADS in format PNG
         */
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
                    val myFile = File("/storage/emulated/0/Download"
                            + File.separator + "DrawingNote" + System.currentTimeMillis()/1000 + ".png")
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
        /**
         * Next fun reports result of image exporting process
         * and
         * after offers to share it with another app
         */
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
            MediaScannerConnection.scanFile(this@MainActivity,
                    arrayOf(result), null){
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
    /**BLOCK ENDS**/


    /**
     * Next two fun responsible for extra menu show/hide
     */
    private fun showExtraMenu(){
        binding.btnRotate.visibility = View.VISIBLE
        binding.btnTrash.visibility = View.VISIBLE
        binding.btnShare.visibility = View.VISIBLE
        binding.btnGallery.visibility = View.VISIBLE
        binding.btnInfo.visibility = View.VISIBLE
        isMenuShown = true
    }
    private fun hideExtraMenu(){
        binding.btnRotate.visibility = View.GONE
        binding.btnTrash.visibility = View.GONE
        binding.btnShare.visibility = View.GONE
        binding.btnGallery.visibility = View.GONE
        binding.btnInfo.visibility = View.GONE
        isMenuShown = false
    }


    /**
     * Next fun responsible for dialog menu of Color picker
     */
    private fun showColorPickerDialog(){
        //last color
        var myColor: String = binding.drawingView.getCurrentColor()
        //create dialog
        val colorDialog = Dialog(this)
        colorDialog.setContentView(R.layout.dialog_color_picker)
        //make background color to transparent. it needs for round corners
        colorDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        colorDialog.setCanceledOnTouchOutside(false)

        //make all touchable object as local
        val myColorPicker = colorDialog.findViewById<ColorPickerView>(R.id.colorPickerView)
        val myLLCurrentColor = colorDialog.findViewById<LinearLayout>(R.id.ll_current_color)
        val tvCurrentHex = colorDialog.findViewById<TextView>(R.id.tv_color_hex)
        //last color as HEX
        tvCurrentHex.text = myColor

        val myOk = colorDialog.findViewById<Button>(R.id.btn_ok_color)
        val myCancel = colorDialog.findViewById<Button>(R.id.btn_cancel_color)

        //show
        colorDialog.show()

        //color listener
        myColorPicker.setColorListener(object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                if (envelope != null) {
                    //show in linear layout current color
                    myLLCurrentColor.setBackgroundColor(envelope.color)
                    tvCurrentHex.text = "#" + envelope.hexCode
                    //save color
                    myColor = envelope.hexCode
                }
            }
        })

        //finish dialog
        myOk.setOnClickListener {
            //implement new color
            binding.drawingView.setColor("#" + myColor)
            colorDialog.dismiss()
        }
        myCancel.setOnClickListener {
            colorDialog.dismiss()
        }
    }

    /**
     * Next fun responsible for dialog menu of Brush size
     */
    private fun showBrushSizeDialog(){
        //create dialog window
        val brushDialog = Dialog(this)
        //initialize dialog layout
        brushDialog.setContentView(R.layout.dialog_brush_size)
        //make background color to transparent. it needs for round corners
        brushDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        brushDialog.setCanceledOnTouchOutside(false)

        /**
         * Next line using for local saving of current states for brush size
         * Needed for better user performance
         */
        //get values from brush dialog to local variables
            //brush size - next changing
        val brushSize = brushDialog.findViewById<SeekBar>(R.id.sb_brush_size)
            //get current brush size to seek bar
        val currentBrushSize: Int = binding.drawingView.getBrushSize().toInt()
        brushSize.progress = 1
        brushSize.progress = textBrushSize
            //text option of brush size
        val displayBrushSize = brushDialog.findViewById<TextView>(R.id.tv_brush_size_display)
            //button of confirmation
        val confirmationBrushSize = brushDialog.findViewById<Button>(R.id.btn_confirm_size)
        //upload brush size
        displayBrushSize.text = "Size: $textBrushSize"

        //display dialog menu
        brushDialog.show()

        /**
         * Next seek bar listener in real time show user's size changes
         */
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
        /**
         * Next listener on OK button implements changes
         * and
         * close dialog menu
         */
        confirmationBrushSize.setOnClickListener {
            //change brush size
            binding.drawingView.setBrushSize(brushSize.progress.toFloat())
            //turn off brush dialog
            brushDialog.dismiss()
        }
    }


    /**
     * Next fun show info about app
     */
    private fun showInfo(){
        //create dialog window
        val infoDialog = Dialog(this)
        //initialize dialog layout
        infoDialog.setContentView(R.layout.dialog_info)
        //make background color to transparent. it needs for round corners
        infoDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val myBack = infoDialog.findViewById<Button>(R.id.btn_back)
        //show dialog
        infoDialog.show()
        //close dialog on button
        myBack.setOnClickListener {
            infoDialog.dismiss()
        }
    }


    /**
     * Next fun changes line color using tag of every color button
     * Initialize directly in xml file
     */
    fun colorClicked(view: View){
        //import button
        val button = view as Button
        //import color using tag of button
        val colorTag = button.tag.toString()
        binding.drawingView.setColor(colorTag)
    }


    /**
     * Next fun make clear all frame
     */
    private fun eraseAll(){
        //erase background image
        binding.ivUsersImage.setImageResource(0)
        //erase lines
        binding.drawingView.removeAllLines()
    }
}