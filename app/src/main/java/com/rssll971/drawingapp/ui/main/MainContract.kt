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

        /**Change visibility to opposite for requested View.
         * Scenarios:
         * - VISIBLE
         * - GONE*/
        fun setViewVisibility(context: Context, v: View, tag: String)

        /** Handle events corresponding to storage permission
         * Responsible only to Gallery*/
        fun checkStoragePermission(context: Context)

        /**Create bitmap from container FrameView.
         * Implements sandwich*/
        fun getBitmapFromView(v: View): Bitmap

        /**Save bitmap in Picture folder using Coroutine.
         * On success call showShareOption(...)*/
        fun onSaveBitmapClick(context: Context, bitmap: Bitmap)

        /**Init every time while app starts, but ONLY in checkNoAdsPurchaseStatus(...)*/
        fun initBillingClient(context: Context)

        /**Request Purchase.
         * - On Success: disableAllAds()*/
        fun requestNoAdsPurchase(activity: Activity, skuDetails: SkuDetails)

        /**Check noAds purchase Status from sharedPreferences.
         * If shp doesn't exist, will create it and after will init initBillingClient(...)
         * NOTE: Status on positive can be changed ONLY in disableAllAds()*/
        fun checkNoAdsPurchaseStatus(activity: Activity, context: Context)
    }
    interface MainView: BaseContract.View{

        /**Set default (0,0) position and reset scaleFactor to View*/
        fun fitFrameView()

        /**Change visibility to requested*/

        fun changeBrushSizeWindowVisibility(visibility: Int)

        /**Change visibility to requested*/
        fun changeExtraOptionsVisibility(visibility: Int)

        /**Show dialog where the user is offered objects for deletion:
         *  - Background
         *  - Lines
         *  - All objects*/
        fun showDeleteDialogChooser()

        fun requestLineRemoving()
        fun requestBackgroundRemoving()
        fun requestBackgroundColorChanging(color: Int)

        /**Show dialog with ColorPicker.
         * User can choose where color will be implemented:
         * - Line
         * - Background*/
        fun showColorPickerDialog()
        fun colorClicked(v: View)

        /**Show dialog with all current Info about app
         * NOTE:
         *  - If user taps 5 times on icon, noAds btn will be available again.
         *  This method is added just in case, if some problems with purchase will occur*/
        fun showInfoDialog()

        /**Run gallery activity, where user can select image for background*/
        fun showGalleryForImage()

        /**This window will appear, after user successfully saved his bitmap*/
        fun showShareOption(path: String)

        /**Will be shown, only if skuDetails != null (in activity)*/
        fun showNoAdsDialog()

        /**Update skuDetails in activity*/
        fun updateSku(skuDetails: SkuDetails)

        /**Request Ads and handle corresponding events*/
        fun initAds()

        /**Change visibility to ads Banner.
         * NOTE: In addition, change noAds purchase Status in sharedPreferences*/
        fun disableAllAds()

        /**Change visibility to ads Banner*/
        fun enableAds()

        fun showProgressDialog()
        fun hideProgressDialog()


        /**If user several times reject storage permission,
         * this bar with app settings button will appear*/
        fun showSnackBarPermissionRequest()
        fun showAppSettings()
        fun showErrorSnackBar()
    }
}