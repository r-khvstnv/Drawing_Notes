package com.rssll971.drawingapp.ui.main

import android.content.Context
import android.view.View
import android.widget.Toast

import androidx.core.view.isVisible
import com.rssll971.drawingapp.R

class MainPresenter: MainContract.Presenter {
    private var view: MainContract.MainView? = null
    private var context: Context? = null

    override fun attach(view: MainContract.MainView) {
        this.view = view
    }
    override fun getContext(context: Context) {
        this.context = context
    }
    override fun detach() {
        this.view = null
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


}