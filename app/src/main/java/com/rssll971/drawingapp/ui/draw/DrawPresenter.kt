/************************************************
 * Created by Ruslan Khvastunov                 *
 * r.khvastunov@gmail.com                       *
 * Copyright (c) 2022                           *
 * All rights reserved.                         *
 *                                              *
 ************************************************/

package com.rssll971.drawingapp.ui.draw

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import com.dinuscxj.gesture.MultiTouchGestureDetector
import com.rssll971.drawingapp.utils.CustomPath

class DrawPresenter: DrawContract.Presenter {
    private var view: DrawContract.DrawView? = null

    /**Draw parameters*/
    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 1f
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    //all created user lines
    private var mPaths = ArrayList<CustomPath>()

    /**MultiTouchDetector*/
    private var mScaleFactor = 1f
    private lateinit var multiTouchDetector: MultiTouchGestureDetector

    override fun attach(view: DrawContract.DrawView) {
        this.view = view
    }

    override fun initTouchDetector(context: Context) {
        multiTouchDetector = MultiTouchGestureDetector(context, MultiTouchListener())
    }

    override fun detach() {
        this.view = null
    }

    override fun setupDrawingOptions() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPath!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onViewSizeChanged(width: Int, height: Int) {
        if (width == 0 || height == 0){
            /**If new size was calculated wrong,
             * canvas will be updated with some delay */
            Handler(Looper.getMainLooper()).postDelayed({
                //update bitmap with current width and height
                mCanvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                canvas = Canvas(mCanvasBitmap!!)
            }, 1000)
        }
        else{
            //update bitmap with current width and height
            mCanvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            canvas = Canvas(mCanvasBitmap!!)
        }
    }

    override fun onDrawRequest(canvas: Canvas?) {
        canvas?.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        //make visible all previous lines
        for (path in mPaths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color

            canvas?.drawPath(path, mDrawPaint!!)
        }

        //set current values for line
        mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
        mDrawPaint!!.color = mDrawPath!!.color

        canvas?.drawPath(mDrawPath!!, mDrawPaint!!)
    }

    override fun touchEventProduced(event: MotionEvent?) {
        if (event != null){
            if (event.pointerCount >= 2)
                multiTouchDetector.onTouchEvent(event)
            else
                drawEventDetector(event = event)
        }
    }

    /**MultiTouchListener*/
    private inner class MultiTouchListener :
        MultiTouchGestureDetector.SimpleOnMultiTouchGestureListener() {
        override fun onScale(detector: MultiTouchGestureDetector?) {
            super.onScale(detector)
            mScaleFactor *= detector?.scale ?: 1f
            /**Min factor should be 1.
             * Otherwise View starts jumping, while user move it or hold fingers on same place*/
            mScaleFactor = 1f.coerceAtLeast(mScaleFactor.coerceAtMost(5f))
            view?.scaleView(mScaleFactor)
        }

        override fun onMove(detector: MultiTouchGestureDetector?) {
            super.onMove(detector)
            val mX = detector?.moveX ?: 0f
            val mY = detector?.moveY ?: 0f
            view?.moveView(mX, mY)
        }
    }

    private fun drawEventDetector(event: MotionEvent?) {
        //store position onTouch
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()

                //start drawing by positions
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                //draw line
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX, touchY)
                    }
                }
            }

            MotionEvent.ACTION_UP ->{
                //add created path in array
                mPaths.add(mDrawPath!!)
                //update values for upcoming line
                mDrawPath = CustomPath(color, mBrushSize)
            }
        }
        view?.invalidateCanvas()
    }

    override fun setBrushSize(newSize: Float){
        if (newSize <= 50)
        mBrushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            Resources.getSystem().displayMetrics)

        mDrawPaint!!.strokeWidth = mBrushSize
    }

    override fun getBrushSize(): Int {
        return mBrushSize.toInt()
    }

    override fun removeLastLine(){
        if (mPaths.size != 0) {
            mPaths.removeAt(mPaths.size - 1)
            view?.invalidateCanvas()
        }
    }

    override fun removeAllLines(){
        if (mPaths.size != 0) {
            mPaths.clear()
            view?.invalidateCanvas()
        }
    }

    override fun setBrushColorFromInt(mColor: Int) {
        color = mColor
        mDrawPaint!!.color = color
    }

    override fun setBrushColorFromString(mColor: String) {
        //parse needed color
        color = Color.parseColor(mColor)
        mDrawPaint!!.color = color
    }

    override fun getCurrentBrushColor(): Int {
        return color
    }

    override fun setPathList(pathList: ArrayList<CustomPath>) {
        mPaths = pathList
        view?.invalidateCanvas()
    }

    override fun getPathList(): ArrayList<CustomPath> {
        return mPaths
    }
}