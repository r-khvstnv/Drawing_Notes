package com.rssll971.drawingapp.ui.draw

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import com.rssll971.drawingapp.di.BaseContract
import com.rssll971.drawingapp.utils.CustomPath

interface DrawContract {
    interface Presenter: BaseContract.Presenter<DrawView>{
        fun setContext(context: Context)
        fun setupDrawingOptions()
        fun onViewSizeChanged(width: Int, height: Int)
        fun onDrawRequest(canvas: Canvas?)
        fun touchEventProduced(event: MotionEvent?)
        fun getBrushSize(): Int
        fun setBrushColorFromInt(mColor: Int)
        fun setBrushColorFromString(mColor: String)
        fun getCurrentBrushColor(): Int
        fun setPathList(pathList: ArrayList<CustomPath>)
        fun getPathList(): ArrayList<CustomPath>
    }
    interface DrawView: BaseContract.View{
        fun invalidateCanvas()
        fun scaleView(scaleFactor: Float)
        fun moveView(x: Float, y: Float)
    }
}