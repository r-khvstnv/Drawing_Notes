package com.rssll971.drawingapp.ui.main

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.android.billingclient.api.*
import com.rssll971.drawingapp.R
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList

class MainPresenter: MainContract.Presenter {
    private var view: MainContract.MainView? = null
    private var context: Context? = null
    private var jobImageSaving = CoroutineScope(Job() + Dispatchers.IO)
    private lateinit var billingClient: BillingClient

    override fun attach(view: MainContract.MainView) {
        this.view = view
    }
    override fun getContext(context: Context) {
        this.context = context
    }
    override fun detach() {
        this.view = null
        jobImageSaving.cancel()
    }

    override fun setViewVisibility(v: View, tag: String) {
        val updatedVisibility = if (v.isVisible)
            View.GONE
        else
            View.VISIBLE

        when(tag){
            context?.getString(R.string.st_extra_options) ->
                view?.changeExtraOptionsVisibility(updatedVisibility)
            context?.getString(R.string.st_brush_size) ->
                view?.changeBrushSizeWindowVisibility(updatedVisibility)
        }
    }

    override fun checkStoragePermission(){
        when{
            ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED -> {
                        view?.showGalleryForImage()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MainActivity.GALLERY_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun getBitmapFromView(v: View): Bitmap {
        val bitmap = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundDrawable = v.background
        if (backgroundDrawable != null)
            backgroundDrawable.draw(canvas)
        else
            canvas.drawColor(Color.WHITE)
        v.draw(canvas)
        return bitmap
    }


    fun onSaveBitmapClick(bitmap: Bitmap){
        jobImageSaving.launch { saveBitmapToStorage(bitmap = bitmap) }
    }
    private suspend fun saveBitmapToStorage(bitmap: Bitmap){
        var resultPath: String? = null
        val directory = Environment.DIRECTORY_PICTURES
        val date = System.currentTimeMillis()
        val fileName = "DN" + date/1000
        val format = ".png"
        withContext(Dispatchers.Main){
            view?.showProgressDialog()
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                val resolver = context?.contentResolver
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + format)
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                contentValues.put(MediaStore.MediaColumns.DATE_ADDED, date)
                contentValues.put(MediaStore.MediaColumns.DATE_MODIFIED, date)
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
                val imageUri = resolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!
                val openOutputStream = resolver.openOutputStream(Objects.requireNonNull(imageUri))
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, openOutputStream)
                Objects.requireNonNull<OutputStream>(openOutputStream)
                resultPath = File(
                    Environment.getExternalStoragePublicDirectory(directory),
                    fileName + format).absolutePath
            } else{
                val bytes = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                val file = File(
                    Environment.getExternalStoragePublicDirectory(directory),
                    fileName + format)
                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.write(bytes.toByteArray())
                fileOutputStream.close()
                resultPath = file.absolutePath
            }

            if (!resultPath.isNullOrEmpty()){
                if (File(resultPath!!).exists()){
                    withContext(Dispatchers.Main){
                        view?.hideProgressDialog()
                        view?.showShareOption(resultPath!!)
                    }
                }
            }
        }.onFailure {
            it.printStackTrace()
            withContext(Dispatchers.Main){
                view?.hideProgressDialog()
                view?.showErrorSnackBar()
            }
        }
    }

    override fun initBillingClient(){
        billingClient = BillingClient.newBuilder(context!!)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        startBillingConnection()
    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK
                && purchases != null){
                for (purchase in purchases){
                    handlePurchase(purchase = purchase)
                }

            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED){
                view?.showErrorSnackBar()
                Log.v("Ads", "Cancelled")
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED){
                view?.disableAllAds()
                Toast.makeText(context, "Already owned", Toast.LENGTH_SHORT).show()
            } else{
                view?.showErrorSnackBar()
            }
        }

    fun startBillingConnection(){
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK){
                    querySkuDetails()
                    queryActivePurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                startBillingConnection()
            }
        })
    }

    fun querySkuDetails(){
        val skuList = ArrayList<String>()
        skuList.add(context?.getString(R.string.no_ads_id)!!)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        billingClient.querySkuDetailsAsync(params.build()){billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK
                && !skuDetailsList.isNullOrEmpty()){
                for (sku in skuDetailsList){
                    view?.updateSku(sku)
                }
            }
        }
    }

    fun queryActivePurchases(){
        billingClient
            .queryPurchasesAsync(BillingClient.SkuType.INAPP){ billingResult, purchases ->
                if(billingResult.responseCode == BillingClient.BillingResponseCode.OK){
                    if (purchases.isNotEmpty()){
                        for (purchase in purchases){
                            Log.i("BILL", purchase.toString())
                            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                                view?.disableAllAds()
                        }
                    } else{
                        Log.v("AdsStatus", "Enabled by qap1")
                        view?.enableAds()
                        Log.v("AdsStatus", "Enabled by qap1")
                    }
                } else{
                    Log.v("AdsStatus", "Enabled by qap2")
                    view?.enableAds()
                }
        }
    }

    private fun handlePurchase(purchase: Purchase){
        if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED){
            if (!purchase.isAcknowledged){
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams){
                    view?.disableAllAds()
                }
            }
        }
    }

    override fun requestNoAdsPurchase(activity: Activity, skuDetails: SkuDetails) {
        val flowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build()
        billingClient.launchBillingFlow(activity, flowParams).responseCode
    }

    override fun checkNoAdsPurchaseStatus(activity: Activity, context: Context) {
        val keyId = context.getString(R.string.no_ads_id)
        val shp = activity.getPreferences(Context.MODE_PRIVATE)
        Log.v("AdsStatus", "Enabled by cnaps")
        if (shp.contains(keyId)){
            //check status based on local data
            if (!shp.getBoolean(keyId, true)){
                //on noPurchase status, ads will be initialized, but no shown
                view?.initAds()
                Log.v("AdsStatus", "init by cnaps")
            }
        } else{
            val editor = shp.edit()
            editor.putBoolean(keyId, false)
            editor.apply()
            view?.initAds()
        }

        //ads will be shown based on remote data
        initBillingClient()
    }
}