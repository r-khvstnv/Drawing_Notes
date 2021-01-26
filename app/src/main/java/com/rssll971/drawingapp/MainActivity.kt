package com.rssll971.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
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
import android.provider.MediaStore
import android.view.*
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dinuscxj.gesture.MultiTouchGestureDetector
import com.google.android.gms.ads.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
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
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        private const val GALLERY_CODE = 104
    }
    /**
     * Other vars
     */
    //variable to store statically text of brush size
    private var textBrushSize: Int = 1
    //var to make scaling of layout
    private var scaleFactor: Float = 1.0f
    //Scale detector. Declare in onCreate, called by button
    private lateinit var myMultiTouchGestureDetector: MultiTouchGestureDetector
    //TODO ADS
    private lateinit var myInterstitialAd: InterstitialAd
    //id of interstitial ad
    private val adInterstitialID: String = "ca-app-pub-3940256099942544/1033173712"

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
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }


    /** ACTIVITY STARTS**/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Declare multi-touch detector
        myMultiTouchGestureDetector = MultiTouchGestureDetector(this, MultiTouchGestureDetectorListener())

        //Prepare and build Ads
        //TODO ADS
        MobileAds.initialize(this)
        myInterstitialAd = InterstitialAd(this)
        myInterstitialAd.adUnitId = adInterstitialID
        myInterstitialAd.loadAd(AdRequest.Builder().build())
        myInterstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                myInterstitialAd.loadAd(AdRequest.Builder().build())
            }
        }

        /**
         * Next line every time set brush size to 1 on first app start
         */
        drawing_view.setBrushSize(1.toFloat())


        /**
         * BLOCK of all listeners for any clickable objects
         */
        //brush
        btn_brush.setOnClickListener { view ->
            showBrushSizeDialog()
        }
        //undo
        btn_undo.setOnClickListener {
            drawing_view.removeLastLine()
        }
        //trash
        btn_trash.setOnClickListener {
            //TODO ADS
            //myInterstitialAd.show()
            //TODO ERASE
            //eraseAll()
            drawing_view.setIsTouchAllowed(true)

        }
        //gallery
        btn_gallery.setOnClickListener{
            //TODO ADS
            //myInterstitialAd.show()
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
        // share/save
        btn_share.setOnClickListener {
            //TODO ADS
            //myInterstitialAd.show()
            if (isPermissionsAreAllowed()){
                BitmapAsyncTask(getBitmapFromView(fl_image_container)).execute()
            }
            else{
                requestPermissions()
            }
        }
        //zoom
        btn_zoom.setOnClickListener {
            /**
             * Next statements responsible for zoom layout displaying
             * for checking right option is using state var zoomButtonPressed,
             * which every time overwritting after success execution
             */
            /*
            if (!zoomButtonPressed){
                ll_zoom.visibility = View.VISIBLE
                zoomButtonPressed = true
                zoomScaling()
            }
            else{
                ll_zoom.visibility = View.GONE
                zoomButtonPressed = false
            }
            */
            drawing_view.setIsTouchAllowed(false)
            //TODO SCALING
            fl_image_container.setOnTouchListener { v, event ->

                myMultiTouchGestureDetector.onTouchEvent(event)
            }
        }

        /** BLOCK ENDS**/
    }

    /**
     * Next inner class implement multi-touch functionality
     *  Takes opportunity to scale and drag frame layout
     */
    private inner class MultiTouchGestureDetectorListener :
        MultiTouchGestureDetector.SimpleOnMultiTouchGestureListener() {
        //TODO ADD COMMENTS
        override fun onScale(detector: MultiTouchGestureDetector?) {
            super.onScale(detector)
            scaleFactor *= detector?.scale ?: 1.0f
            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 5.0f))
            fl_image_container.scaleX = scaleFactor
            fl_image_container.scaleY = scaleFactor
            fl_image_container.invalidate()
        }

        override fun onMove(detector: MultiTouchGestureDetector?) {
            super.onMove(detector)
            fl_image_container.x += detector?.moveX ?: 0.0f
            fl_image_container.y += detector?.moveY ?: 0.0f
            fl_image_container.invalidate()
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
    /**
     * Next fun check permissions availability for app
     * */
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
    /**BLOCK ENDS**/



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
        val brushSize = brushDialog.sb_brush_size
            //get current brush size to seek bar
        val currentBrushSize: Int = drawing_view.getBrushSize().toInt()
        brushSize.progress = 1
        brushSize.progress = textBrushSize
            //text option of brush size
        val displayBrushSize = brushDialog.tv_brush_size_display
            //button of confirmation
        val confirmationBrushSize = brushDialog.btn_confirm_size
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
            drawing_view.setBrushSize(brushSize.progress.toFloat())
            //turn off brush dialog
            brushDialog.dismiss()
        }
    }


    /**
     * Next fun responsible for frame scaling (image + bitmap) to:
     *  -custom size
     *  -default size
     * Changes are made sb_layout_size and fun calls using btn_zoom
     */
    private fun zoomScaling(){
        //import current states for sb and text
        sb_layout_size.progress = (scaleFactor*100.0f).toInt()
        tv_current_scale.text = "${sb_layout_size.progress}%"

        /**
         * Next seek bar listener in real time shows and implements user's changes
         */
        sb_layout_size.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sb_layout_size.progress = progress
                tv_current_scale.text = "${progress}%"
                scaleFactor = (progress.toFloat()/100)
                //implement scaling in real time
                fl_image_container.scaleX = scaleFactor
                fl_image_container.scaleY = scaleFactor
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        /**
         * Next listener makes frame size by default
         */
        tv_reset_scale.setOnClickListener {
            scaleFactor = 1.0f
            sb_layout_size.progress = 100
            tv_current_scale.text = "100%"
            fl_image_container.scaleX = scaleFactor
            fl_image_container.scaleY = scaleFactor
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
        drawing_view.setColor(colorTag)
    }


    /**
     * Next fun make clear all frame
     */
    private fun eraseAll(){
        //erase background image
        iv_users_image.setImageResource(0)
        //erase lines
        drawing_view.removeAllLines()
    }
}