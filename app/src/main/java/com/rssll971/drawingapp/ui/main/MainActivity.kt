package com.rssll971.drawingapp.ui.main


import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.rssll971.drawingapp.R
import com.rssll971.drawingapp.databinding.ActivityMainBinding
import com.rssll971.drawingapp.di.ActivityModule
import com.rssll971.drawingapp.di.DaggerActivityComponent
import javax.inject.Inject


class MainActivity : AppCompatActivity(), MainContract.MainView {
    @Inject lateinit var presenter: MainPresenter
    private lateinit var binding: ActivityMainBinding

    //ADS
    //banner ad
    private lateinit var myBannerAdView: AdView
    //interstitial
    private var mInterstitialAd: InterstitialAd? = null
    //firebase
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * Next two method enable fullscreen mode and transparent navigation/status bars
     */
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


    private fun injector(){
        val injectorMainComponent =
            DaggerActivityComponent.builder()
                .activityModule(ActivityModule(this)).build()
        injectorMainComponent.inject(this)
    }
    /** ACTIVITY STARTS**/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val  view = binding.root
        setContentView(view)
        injector()
        presenter.attach(this)
        presenter.getContext(this)
        /** Firebase*/
        //firebaseAnalytics = Firebase.analytics todo enable
        //crutch when systemUI doesn't disappear
        Handler(Looper.getMainLooper()).postDelayed({onWindowFocusChanged(true)}, 1000)

        /** Prepare and build Ads*/
        prepareAds()

        with(binding){//todo functionality buttons
            //primary buttons
            btnFit.setOnClickListener { this@MainActivity.fitFrameView() }
            btnUndo.setOnClickListener {  }
            btnBrushSize.setOnClickListener { presenter.setViewVisibility(llBrushSizeWindow, it.tag.toString()) }
            btnPalette.setOnClickListener {  }

            //extra menu
            btnExtraOptions.setOnClickListener { presenter.setViewVisibility(llExtraOptions, it.tag.toString()) }
            btnTrash.setOnClickListener {  }
            btnShare.setOnClickListener {  }
            btnGallery.setOnClickListener {  }
            btnInfo.setOnClickListener { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detach()
    }






    /**
     * Next 3 methods provide all functionality relative to ads
     * */
    private fun prepareAds(){
        //interstitial
        requestInterstitialAd()
        //banner ads
        MobileAds.initialize(this)
        myBannerAdView = findViewById(R.id.adView_banner)
        myBannerAdView.loadAd(AdRequest.Builder().build())
        myBannerAdView.adListener = object : AdListener(){
            override fun onAdClosed() {
                myBannerAdView.loadAd(AdRequest.Builder().build())
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

    fun colorClicked(v: View) {
        binding.drawingView.presenter.setColor(v.tag.toString())
    }
}