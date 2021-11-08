package com.rssll971.drawingapp.ui.main

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.nfc.Tag
import android.view.View
import com.android.billingclient.api.SkuDetails
import com.rssll971.drawingapp.di.BaseContract

interface MainContract {
    interface Presenter: BaseContract.Presenter<MainView>{
        fun getContext(context: Context)
        fun setViewVisibility(v: View, tag: String)
        fun checkStoragePermission()
        fun getBitmapFromView(v: View): Bitmap
        fun initBillingClient()
        fun requestNoAdsPurchase(activity: Activity, skuDetails: SkuDetails)
        fun checkNoAdsPurchaseStatus(activity: Activity, context: Context)
    }
    interface MainView: BaseContract.View{
        fun fitFrameView()
        fun changeBrushSizeWindowVisibility(visibility: Int)
        fun changeExtraOptionsVisibility(visibility: Int)
        fun showDeleteDialogChooser()
        fun requestLineRemoving()
        fun requestBackgroundRemoving()
        fun showColorPickerDialog()
        fun requestBackgroundColorChanging(color: Int)
        fun showInfoDialog()
        fun showSnackBarPermissionRequest()
        fun showAppSettings()
        fun showGalleryForImage()
        fun showShareOption(path: String)
        fun showNoAdsDialog()
        fun updateSku(skuDetails: SkuDetails)
        fun showErrorSnackBar()
        fun disableAllAds()
        fun enableAds()
        fun initAds()
    }
}