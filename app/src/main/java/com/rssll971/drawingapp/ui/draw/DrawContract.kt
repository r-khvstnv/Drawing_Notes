package com.rssll971.drawingapp.ui.draw

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import com.rssll971.drawingapp.di.BaseContract

interface DrawContract {
    interface Presenter: BaseContract.Presenter<DrawView>{
        fun setContext(context: Context)
        fun setupDrawingOptions()
        fun onViewSizeChanged(width: Int, height: Int)
        fun onDrawRequest(canvas: Canvas?)
        fun touchEventProduced(event: MotionEvent?)
    }
    interface DrawView: BaseContract.View{
        fun invalidateCanvas()
        fun scaleView(scaleFactor: Float)
        fun moveView(x: Float, y: Float)
    }
}