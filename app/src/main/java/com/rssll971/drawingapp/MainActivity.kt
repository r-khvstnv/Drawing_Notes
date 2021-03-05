package com.rssll971.drawingapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import com.dinuscxj.gesture.MultiTouchGestureDetector
import com.google.android.gms.ads.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
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
     * Permission code for gallery
     */
    companion object{
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
     * Next two method enable fullscreen mode and transparent navigation/status bars
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

        /** change orientation state and lock screen rotation**/
        when(requestedOrientation){
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {isPortraitMode = true
                Toast.makeText(this, "Portrait Mode", Toast.LENGTH_LONG).show()}
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {isPortraitMode = false
                Toast.makeText(this, "Landscape Mode", Toast.LENGTH_LONG).show()}
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED


        /** Hide all unnecessary layouts*/
        hideExtraMenu()
        binding.llBrushSizeChanger.visibility = View.GONE
        //crutch when systemUI doesn't disappear
        Handler().postDelayed({onWindowFocusChanged(true)}, 1000)


        //Declare multi-touch detector
        myMultiTouchGestureDetector = MultiTouchGestureDetector(
                this, MultiTouchGestureDetectorListener())


        /** Prepare and build Ads*/
        MobileAds.initialize(this)

        myInterstitialAd = InterstitialAd(this)
        myInterstitialAd.adUnitId = adInterstitialID
        myBannerAdView = findViewById(R.id.adView_smart_banner)
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


        /** Next line set brush size to 1 on first app start*/
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
            binding.btnActiveDraw.setBackgroundResource(R.drawable.ib_option_grey_light)
            binding.btnTransfer.setBackgroundResource(R.drawable.ib_option_white)
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
            binding.btnActiveDraw.setBackgroundResource(R.drawable.ib_option_white)
            binding.btnTransfer.setBackgroundResource(R.drawable.ib_option_grey_light)
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
        binding.btnBrushSize.setOnClickListener {
            if (binding.llBrushSizeChanger.isVisible){
                binding.llBrushSizeChanger.visibility = View.GONE
                binding.btnBrushSize.setBackgroundResource(R.drawable.ib_option_white)
            }
            else{
                binding.llBrushSizeChanger.visibility = View.VISIBLE
                binding.btnBrushSize.setBackgroundResource(R.drawable.ib_option_grey_light)
                showBrushSizeDialog()
            }
        }
        //undo
        binding.btnUndo.setOnClickListener {
            binding.drawingView.removeLastLine()
        }
        //extra menu
        binding.btnExtraMenu.setOnClickListener {
            if (binding.btnRotate.isVisible){
                hideExtraMenu()
            }
            else{
                //show menu and ads
                myInterstitialAd.show()
                showExtraMenu()
            }
        }
        /** screen orientation */
        binding.btnRotate.setOnClickListener {
            requestedOrientation = if (isPortraitMode){
                //switch to landscape
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else{
                //switch to portrait
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
            defaultScaleAndPosition()
            hideExtraMenu()
            externalProcessesWithImage(it.tag.toString())
        }
        //gallery
        binding.btnGallery.setOnClickListener{
            defaultScaleAndPosition()
            hideExtraMenu()
            externalProcessesWithImage(it.tag.toString())
        }
        //info
        binding.btnInfo.setOnClickListener {
            hideExtraMenu()
            showInfo()
        }
        /** BLOCK ENDS**/

    }

    /**
     * Next inner class implement multi-touch functionality to scale and drag frame layout
     */
    private inner class MultiTouchGestureDetectorListener :
        MultiTouchGestureDetector.SimpleOnMultiTouchGestureListener() {
        //scale frame method
        override fun onScale(detector: MultiTouchGestureDetector?) {
            super.onScale(detector)
            scaleFactor *= detector?.scale ?: 1.0f
            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 5.0f))
            binding.flImageContainer.scaleX = scaleFactor
            binding.flImageContainer.scaleY = scaleFactor
        }
        //drag frame method
        override fun onMove(detector: MultiTouchGestureDetector?) {
            super.onMove(detector)
            binding.flImageContainer.x += detector?.moveX ?: 0.0f
            binding.flImageContainer.y += detector?.moveY ?: 0.0f
        }
    }
    /**
     * Next method fit frame to default sizes
     */
    private fun defaultScaleAndPosition(){
        //default size
        scaleFactor = 1.0f
        binding.flImageContainer.scaleX = scaleFactor
        binding.flImageContainer.scaleY = scaleFactor
        //for portrait mode
        if (isPortraitMode) {
            /** Align all frame relativity to adBanner,
             * due to it top object in portrait mode*/
            binding.flImageContainer.x = myBannerAdView.left.toFloat()
            binding.flImageContainer.y = myBannerAdView.bottom.toFloat()
        }
        else{
            binding.flImageContainer.x = 0.0f
            binding.flImageContainer.y = 0.0f
        }
    }


    /**
     * Next dialog will be shown, if previously user reject all permissions
     * required to gallery & share features
     */
    private fun showRationalPermissionDialog(){
        val permissionAlertDialog = AlertDialog.Builder(this).setMessage(R.string.st_permission_needed_to_be_granted)
        //positive button
        permissionAlertDialog.setPositiveButton(getString(R.string.st_go_to_settings)){ _: DialogInterface, _: Int ->
            try {
                //move to app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }catch (e: ActivityNotFoundException){
                e.printStackTrace()
            }
        }
        //negative button
        permissionAlertDialog.setNegativeButton(R.string.st_cancel){ dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
        }
        //show
        permissionAlertDialog.show()
    }
    /**
     * Next method check permissions availability for specific action
     * and makes decision for next processing
     * */
    private fun externalProcessesWithImage(tagString: String){
        Dexter.withContext(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(object: MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()){
                    when(tagString){
                        /** Action for picking image from gallery*/
                        getString(R.string.st_gallery) -> {
                            val pickImageIntent = Intent(Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            startActivityForResult(pickImageIntent, GALLERY_CODE)
                        }
                        /** Action for storing image to downloads folder*/
                        getString(R.string.st__share) -> {
                            BitmapAsyncTask(getBitmapFromView(binding.flImageContainer)).execute()
                        }
                        /** Fatal error*/
                        else -> Log.e("ExternalImageProcess", "Unknown operation")
                    }
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                    permissionsList: MutableList<PermissionRequest>?,
                    permissionToken: PermissionToken?) {
                showRationalPermissionDialog()
            }

        }).onSameThread().check()
    }



    /**
     * Next method extracts user image from gallery and substitutes data in image view
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == GALLERY_CODE){
                //get user image as background, using exception
                try {
                    //check for available data
                    if (data!!.data != null){
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
     * Next method makes sandwich with bitmap and image
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
     * Next Class implements AsyncTask and responsible for all process with image exporting/sharing
     */
    private inner class BitmapAsyncTask(val myBitmap: Bitmap) : AsyncTask<Any, Void, String>(){
        //Loading simple dialog
        var loadingDialog = Dialog(this@MainActivity)
        //show progress, while image is exporting
        fun showProgressDialog(){
            loadingDialog.setContentView(R.layout.dialog_background_progress)
            loadingDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
         * Next method saves prepared image in Folder DOWNLOADS in format PNG
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
         * Next method reports result of image exporting process
         * and
         * offers to share it with another app
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
            //share image to another app
            MediaScannerConnection.scanFile(this@MainActivity,
                    arrayOf(result), null){
                _, uri -> val sharingIntent = Intent()
                sharingIntent.action = Intent.ACTION_SEND
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
                sharingIntent.type = "image/png"
                startActivity(
                        Intent.createChooser( sharingIntent, "Share")
                )
            }
        }
    }


    /**
     * Next two methods show/hide extra bottom-right menu
     */
    private fun showExtraMenu(){
        binding.btnRotate.visibility = View.VISIBLE
        binding.btnTrash.visibility = View.VISIBLE
        binding.btnShare.visibility = View.VISIBLE
        binding.btnGallery.visibility = View.VISIBLE
        binding.btnInfo.visibility = View.VISIBLE
    }
    private fun hideExtraMenu(){
        binding.btnRotate.visibility = View.GONE
        binding.btnTrash.visibility = View.GONE
        binding.btnShare.visibility = View.GONE
        binding.btnGallery.visibility = View.GONE
        binding.btnInfo.visibility = View.GONE
    }


    /**
     * Next method show dialog menu of Color picker
     */
    private fun showColorPickerDialog(){
        //last color
        var colorHex: String = binding.drawingView.getCurrentColor()
        var colorInt = 0
        //create dialog
        val colorDialog = Dialog(this)
        colorDialog.setContentView(R.layout.dialog_color_picker)
        //make background color to transparent. it needs for round corners
        colorDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        //make all touchable object as local
        val myColorPicker = colorDialog.findViewById<ColorPickerView>(R.id.colorPickerView)
        val llCurrentColor = colorDialog.findViewById<LinearLayout>(R.id.ll_current_color)
        val llLineColor = colorDialog.findViewById<LinearLayout>(R.id.ll_line_color)
        val llSolidColor = colorDialog.findViewById<LinearLayout>(R.id.ll_solid_color)

        //show
        colorDialog.show()

        //color listener
        myColorPicker.setColorListener(object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                if (envelope != null) {
                    //show in linear layout current color
                    llCurrentColor.setBackgroundColor(envelope.color)
                    /** for background color changing*/
                    colorInt = envelope.color
                    /** for line color changing*/
                    colorHex = envelope.hexCode
                }
            }
        })

        //color for lines
        llLineColor.setOnClickListener {
            //implement new color
            binding.drawingView.setColor("#$colorHex")
            colorDialog.dismiss()
        }
        //color for background
        llSolidColor.setOnClickListener {
            binding.ivUsersImage.setBackgroundColor(colorInt)
            colorDialog.dismiss()
        }
    }

    /**
     * Next method show dialog menu of Brush size
     */
    private fun showBrushSizeDialog(){
        /**
         * Next line load current value of brush size
         * Needed for better user performance
         */
        binding.sbBrushSize.progress = textBrushSize
        //set text variant of brush size
        binding.tvBrushSizeDisplay.text = "${getString(R.string.st_size)} $textBrushSize"

        /**
         * Next seek bar listener in real time show user's size changes and implement them
         */
        binding.sbBrushSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                textBrushSize = i
                binding.tvBrushSizeDisplay.text = "${getString(R.string.st_size)} $textBrushSize"
                binding.drawingView.setBrushSize(i.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    }


    /**
     * Next method show info about app
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
     * Next method changes line color using tag of every color button
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
     * Next method remove all lines and background image
     */
    private fun eraseAll(){
        //erase background image
        binding.ivUsersImage.setImageResource(0)
        //erase lines
        binding.drawingView.removeAllLines()
    }
}