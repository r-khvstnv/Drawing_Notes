package com.rssll971.drawingapp.ui.main


import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.rssll971.drawingapp.R
import com.rssll971.drawingapp.databinding.*
import com.rssll971.drawingapp.di.ActivityModule
import com.rssll971.drawingapp.di.DaggerActivityComponent
import com.rssll971.drawingapp.utils.CustomPath
import com.rssll971.drawingapp.utils.GalleryContract
import com.rssll971.drawingapp.utils.MyConstants
import com.skydoves.colorpickerview.listeners.ColorListener
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

class MainActivity : AppCompatActivity(), MainContract.MainView {
    @Inject lateinit var presenter: MainPresenter
    private lateinit var binding: ActivityMainBinding

    private var mInterstitialAd: InterstitialAd? = null
    //sku no ads
    private var skuDetails: SkuDetails? = null

    //firebase
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    //progress dialog
    private lateinit var progressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val  view = binding.root
        setContentView(view)
        injector()
        presenter.attach(this)
        /** Firebase*/
        firebaseAnalytics = Firebase.analytics

        initBrushSizeListener()

        presenter.shouldShowAdsRationale(this, this)

        //Handler(Looper.getMainLooper()).postDelayed({onWindowFocusChanged(true)}, 1000)

        with(binding){
            //primary buttons
            btnNoAds.setOnClickListener { showNoAdsDialog() }
            btnFit.setOnClickListener { fitFrameView() }
            btnUndo.setOnClickListener { binding.drawingView.presenter.removeLastLine() }
            btnBrushSize.setOnClickListener {
                presenter.setViewVisibility(
                    this@MainActivity, llBrushSizeWindow, it.tag.toString())
            }
            btnPalette.setOnClickListener { showColorPickerDialog() }

            //extra menu
            btnExtraOptions.setOnClickListener {
                showInterstitialAd()
                presenter.setViewVisibility(
                    this@MainActivity, llExtraOptions, it.tag.toString())
            }
            btnTrash.setOnClickListener {
                changeExtraOptionsVisibility(View.GONE)
                showDeleteDialogChooser()
            }
            btnShare.setOnClickListener {
                changeExtraOptionsVisibility(View.GONE)
                lifecycleScope.launch {
                    presenter.saveBitmapToStorage(
                        this@MainActivity,
                        presenter.getBitmapFromView(binding.flContainer)
                    )
                }
            }
            btnGallery.setOnClickListener {
                changeExtraOptionsVisibility(View.GONE)
                presenter.checkStoragePermission(this@MainActivity)
            }
            btnInfo.setOnClickListener {
                changeExtraOptionsVisibility(View.GONE)
                showInfoDialog()
            }
        }
    }

    private fun injector(){
        val injectorMainComponent =
            DaggerActivityComponent.builder()
                .activityModule(ActivityModule(this)).build()
        injectorMainComponent.inject(this)
    }

    /**Enable fullscreen mode and transparent navigation/status bars*/
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        } else{
            @Suppress("DEPRECATION")
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
    }

    /**Save current width & height of frameView container and pathList */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val list = binding.drawingView.presenter.getPathList()
        outState.putParcelableArrayList(MyConstants.PATH_LIST_KEY, list)
        if (list.isNotEmpty()){
            val width = binding.flContainer.width
            val height = binding.flContainer.height
            outState.putInt(MyConstants.CONTAINER_WIDTH_KEY, width)
            outState.putInt(MyConstants.CONTAINER_HEIGHT_KEY, height)
        }
    }

    /**Restore saved width & height of frameView container and pathList */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val list = savedInstanceState
            .getParcelableArrayList<CustomPath>(MyConstants.PATH_LIST_KEY)!!
        /**If data exist, it will be restored. Otherwise will be used default view
         * for current state*/
        if (list.isNotEmpty()){
            val width = savedInstanceState.getInt(MyConstants.CONTAINER_WIDTH_KEY)
            val height = savedInstanceState.getInt(MyConstants.CONTAINER_HEIGHT_KEY)
            binding.flContainer.layoutParams = ViewGroup.LayoutParams(width, height)
        }

        binding.drawingView.presenter.setPathList(list)
    }


    /*--- ADS*/
    override fun initAds(){
        //interstitial
        requestInterstitialAd()
        //banner ads
        MobileAds.initialize(this)
        binding.adViewBanner.loadAd(AdRequest.Builder().build())
        binding.adViewBanner.adListener = object : AdListener(){
            override fun onAdClosed() {
                binding.adViewBanner.loadAd(AdRequest.Builder().build())
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                binding.adViewBanner.loadAd(AdRequest.Builder().build())
            }
        }
    }
    private fun requestInterstitialAd(){
        val adRequest = AdRequest.Builder().build()
        loadInterstitialAd(adRequest)
    }
    private fun loadInterstitialAd(adRequest: AdRequest){
        InterstitialAd.load(this, getString(R.string.add_interstitial_ID),
            adRequest, object : InterstitialAdLoadCallback(){
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("AdMob", adError.message)
                mInterstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd

                mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        requestInterstitialAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                        requestInterstitialAd()
                    }

                    override fun onAdShowedFullScreenContent() {
                        mInterstitialAd = null
                    }
                }
            }
        })
    }
    private fun showInterstitialAd(){
        if (mInterstitialAd != null)
            mInterstitialAd!!.show(this)
    }

    override fun enableAds() {
        this.runOnUiThread {
            binding.adsContainer.visibility = View.VISIBLE
            binding.btnNoAds.visibility = View.VISIBLE
        }
    }

    override fun disableAllAds() {
        val pref = this.getPreferences(Context.MODE_PRIVATE)
        if (!pref.getBoolean(MyConstants.IN_APP_PURCHASE_ID, false)){
            val editor = pref.edit()
            editor.putBoolean(MyConstants.IN_APP_PURCHASE_ID, true)
            editor.apply()
        }

        this.runOnUiThread{
            binding.adsContainer.visibility = View.GONE
            binding.btnNoAds.visibility = View.GONE
        }
    }
    /*--- ADS*/

    private fun initBrushSizeListener(){
        binding.sbBrushSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.drawingView.presenter.setBrushSize(progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun fitFrameView() {
        binding.flContainer.apply {
            scaleX = 1f
            scaleY = 1f
            x = 0f
            y = 0f
        }
    }

    override fun changeBrushSizeWindowVisibility(visibility: Int) {
        binding.llBrushSizeWindow.visibility = visibility
    }

    override fun changeExtraOptionsVisibility(visibility: Int) {
        binding.llExtraOptions.visibility = visibility
    }

    override fun showDeleteDialogChooser() {
        val dialog = Dialog(this)
        val dBinding = DialogDeleteBinding.inflate(LayoutInflater.from(this))
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(dBinding.root)

        with(dBinding){
            btnRemoveLines.setOnClickListener {
                requestLineRemoving()
                dialog.dismiss()
            }
            btnRemoveBackground.setOnClickListener {
                requestBackgroundRemoving()
                dialog.dismiss()
            }
            btnRemoveAll.setOnClickListener {
                requestLineRemoving()
                requestBackgroundRemoving()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun requestLineRemoving() {
        binding.drawingView.presenter.removeAllLines()
    }

    override fun requestBackgroundRemoving() {
        binding.ivUsersImage.setImageResource(0)
        binding.ivUsersImage.setBackgroundColor(ContextCompat.getColor(this, R.color.mWhite))
    }

    override fun colorClicked(v: View) {
        binding.drawingView.presenter.setBrushColorFromString(v.tag.toString())
    }

    override fun showColorPickerDialog() {
        val dialog = Dialog(this)
        val dBinding = DialogColorPickerBinding.inflate(LayoutInflater.from(this))
        var mColor = binding.drawingView.presenter.getCurrentBrushColor()
        var isChangedByUser = false
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(dBinding.root)

        with(dBinding){
            colorPickerView.setColorListener(object : ColorListener{
                override fun onColorSelected(color: Int, fromUser: Boolean) {
                    mColor = color
                    isChangedByUser = fromUser
                    llCurrentColor.setBackgroundColor(color)
                }
            })
            llLineColor.setOnClickListener {
                binding.drawingView.presenter.setBrushColorFromInt(mColor)
                dialog.dismiss()
            }
            llSolidColor.setOnClickListener {
                if (isChangedByUser)
                    requestBackgroundColorChanging(mColor)

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun requestBackgroundColorChanging(color: Int) {
        binding.ivUsersImage.setBackgroundColor(color)
    }

    override fun showInfoDialog() {
        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.dialog_info)

        /*Additional way to get in noAds Dialog,
        if noAds was purchased, but some problems are occurred*/
        var counter = 0
        val ivIcon = dialog.findViewById<ImageView>(R.id.iv_icon)

        ivIcon.setOnClickListener {
            counter++
            if (counter == 5){
                binding.btnNoAds.visibility = View.VISIBLE
                Toast.makeText(this, "Button Unblocked", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        presenter.onPermissionsResult(
            requestCode = requestCode,
            permissions = permissions,
            grantResults = grantResults)
    }

    override fun showSnackBarPermissionRequest() {
        val sb = Snackbar.make(
            findViewById(android.R.id.content),
            getString(R.string.st_permission_needed_to_be_granted),
            Snackbar.LENGTH_LONG)
        sb.view.setBackgroundColor(ContextCompat.getColor(this, R.color.mGreyDark))
        sb.setActionTextColor(ContextCompat.getColor(this, R.color.mWhite))
        sb.setAction(getString(R.string.st_settings)){
            showAppSettings()
        }
        sb.show()
    }

    override fun showAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private val galleryLauncher = registerForActivityResult(GalleryContract()){ uri: Uri? ->
        presenter.onGalleryLauncherResult(uri = uri)
    }

    override fun showGalleryForImage() {
        galleryLauncher.launch("image/*")
    }

    override fun showShareOption(path: String) {
        MediaScannerConnection.scanFile(
            this@MainActivity,
            arrayOf(path), null){
                _, uri ->

            val sharingIntent = Intent()
            sharingIntent.action = Intent.ACTION_SEND
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
            sharingIntent.type = "image/png"

            startActivity(
                Intent.createChooser( sharingIntent, "Share"))
        }
    }

    override fun updateSku(skuDetails: SkuDetails) {
        this.skuDetails = skuDetails
    }
    override fun showNoAdsDialog() {
        if (skuDetails != null){
            val dialog = Dialog(this)
            val dBinding = DialogNoAdsPurchaseBinding.inflate(LayoutInflater.from(this))
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setContentView(dBinding.root)

            with(dBinding){
                //tvNoAdsTitle.text = skuDetails!!.title
                tvNoAdsDescription.text = skuDetails!!.description
                btnBuy.text = skuDetails!!.price

                btnBuy.setOnClickListener {
                    presenter.requestNoAdsPurchase(
                        this@MainActivity,
                        skuDetails = skuDetails!!)
                    dialog.dismiss()
                }
            }

            dialog.show()
        }
    }

    override fun showErrorSnackBar() {
        val sb = Snackbar.make(
            findViewById(android.R.id.content),
            getString(R.string.st_goes_wrong),
            Snackbar.LENGTH_LONG)
        sb.view.setBackgroundColor(ContextCompat.getColor(this, R.color.mGreyDark))
        sb.setTextColor(ContextCompat.getColor(this, R.color.mWhite))
        sb.show()
    }



    override fun showProgressDialog() {
        progressDialog = Dialog(this)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog.window?.setGravity(Gravity.TOP)
        progressDialog.setContentView(R.layout.dialog_progress)
        progressDialog.show()
        lifecycleScope
    }

    override fun hideProgressDialog() {
        progressDialog.dismiss()
    }

    override fun setUserImage(uri: Uri) {
        binding.ivUsersImage.setImageURI(uri)
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }
}