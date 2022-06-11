/************************************************
 * Created by Ruslan Khvastunov                 *
 * r.khvastunov@gmail.com                       *
 * Copyright (c) 2022                           *
 * All rights reserved.                         *
 *                                              *
 ************************************************/

package com.rssll971.drawingapp.ui.draw

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import com.rssll971.drawingapp.di.BaseContract
import com.rssll971.drawingapp.utils.CustomPath

interface DrawContract {
    interface Presenter: BaseContract.Presenter<DrawView>{

        /**Method init MultiTouchDetector*/
        fun initTouchDetector(context: Context)

        fun setupDrawingOptions()

        /**Method will update bitmap, if View size is changed*/
        fun onViewSizeChanged(width: Int, height: Int)

        /**Method handle to process related to drawing*/
        fun onDrawRequest(canvas: Canvas?)

        /**Method recognize what event has been requested by user
         * If:
         *  SingleTouch - request Draw method
         *  DoubleTouch - request Move&Scale method*/
        fun touchEventProduced(event: MotionEvent?)

        /** Change brush size proportionally for any screen.
         * End value - DP*/
        fun setBrushSize(newSize: Float)
        fun getBrushSize(): Int
        fun setBrushColorFromInt(mColor: Int) //called by colorPicker
        fun setBrushColorFromString(mColor: String) //called by btn press
        fun getCurrentBrushColor(): Int

        fun removeLastLine()
        fun removeAllLines()

        /**Methods are used for saving&restoring all user lines,
         * if corresponding method has been called*/
        fun setPathList(pathList: ArrayList<CustomPath>)
        fun getPathList(): ArrayList<CustomPath>
    }
    interface DrawView: BaseContract.View{

        /**Invalidate all paths*/
        fun invalidateCanvas()

        /** Scale parentView (FrameView) by received factor.
         * Due to, under DrawingView located ImageContainer method applied on parentView.
         * Accordingly size will be changed for all elements*/
        fun scaleView(scaleFactor: Float)

        /** Move parentView (FrameView).
         * Due to, under DrawingView located ImageContainer method applied on parentView.
         * Accordingly position will be changed for all elements*/
        fun moveView(x: Float, y: Float)
    }
}