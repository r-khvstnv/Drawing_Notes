package com.rssll971.drawingapp.ui.main

import android.content.Context
import android.graphics.Canvas
import android.nfc.Tag
import android.view.View
import com.rssll971.drawingapp.di.BaseContract

interface MainContract {
    interface Presenter: BaseContract.Presenter<MainView>{
        fun getContext(context: Context)
        fun setViewVisibility(v: View, tag: String)
        //todo color picker
        //todo share
        //todo info

    }
    interface MainView: BaseContract.View{
        //todo interstitial ad
        //todo rewarded ad
        fun fitFrameView()
        fun changeBrushSizeWindowVisibility(visibility: Int)
        fun changeExtraOptionsVisibility(visibility: Int)
        fun showDeleteDialogChooser()
        fun requestLineRemoving()
        fun requestBackgroundRemoving()
        fun showColorPickerDialog()
        fun requestBackgroundColorChanging(color: Int)
        fun showInfoDialog()
    }
}