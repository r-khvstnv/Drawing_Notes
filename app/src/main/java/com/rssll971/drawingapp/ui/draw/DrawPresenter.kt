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
    /**
     * BLOCK with all vars for drawing
     */
    //path of sth drawing
    private var mDrawPath: CustomPath? = null
    //map of current path
    private var mCanvasBitmap: Bitmap? = null
    //styles of drawing path
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null

    //thickness of brush
    private var mBrushSize: Float = 0.toFloat()
    //color of drawing (by default)
    private var color = Color.BLACK

    //canvas - холст
    private var canvas: Canvas? = null
    //all created paths
    private var mPaths = ArrayList<CustomPath>()

    //multi-touch
    private var mScaleFactor = 1f
    private lateinit var multiTouchDetector: MultiTouchGestureDetector

    override fun attach(view: DrawContract.DrawView) {
        this.view = view
    }

    override fun setTouchDetector(context: Context) {
        multiTouchDetector = MultiTouchGestureDetector(context, MultiTouchListener())
    }

    override fun detach() {
        this.view = null
    }

    override fun setupDrawingOptions() {
        //object of paint
        mDrawPaint = Paint()
        //styles of drawing implemented inner class
        mDrawPath = CustomPath(color, mBrushSize)
        //color
        mDrawPath!!.color = color
        //type of drawing is line
        mDrawPaint!!.style = Paint.Style.STROKE
        //style of line ends
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        //copy graphic bit from one part to another
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onViewSizeChanged(width: Int, height: Int) {
        if (width == 0 || height == 0){
            Handler(Looper.getMainLooper()).postDelayed({
                //create bitmap with current width and height, using this config of colors
                mCanvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                //create canvas with our bitmap
                canvas = Canvas(mCanvasBitmap!!)
            }, 1000)
        }
        else{
            //create bitmap with current width and height, using this config of colors
            mCanvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            //create canvas with our bitmap
            canvas = Canvas(mCanvasBitmap!!)
        }
    }

    override fun onDrawRequest(canvas: Canvas?) {
        //implement our canvasBitmap, starting on top-left using our canvasPaint
        canvas?.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        /**
         * Next loop make visible all previous lines
         */
        for (path in mPaths){
            //thickness
            mDrawPaint!!.strokeWidth = path.brushThickness
            //color
            mDrawPaint!!.color = path.color
            //draw
            canvas?.drawPath(path, mDrawPaint!!)
        }
        /**
         * Next lines set current values for line and draw it
         */
        //thickness
        mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
        //color
        mDrawPaint!!.color = mDrawPath!!.color
        //draw path
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

    private inner class MultiTouchListener :
        MultiTouchGestureDetector.SimpleOnMultiTouchGestureListener() {
        override fun onScale(detector: MultiTouchGestureDetector?) {
            super.onScale(detector)
            mScaleFactor *= detector?.scale ?: 1f
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
        /** Next statement is crutch for conflict described upper*/
        //store position onTouch
        val touchX = event?.x
        val touchY = event?.y

        //what should will be executed onTouch
        when(event?.action){
            //press on the screen
            MotionEvent.ACTION_DOWN ->{
                //setup path
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                //delete any path
                mDrawPath!!.reset()

                //start drawing by positions
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }
            }

            //drag over the screen
            MotionEvent.ACTION_MOVE ->{
                //draw line
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX, touchY)
                    }
                }
            }

            //press gesture
            MotionEvent.ACTION_UP ->{
                //add created path in array
                mPaths.add(mDrawPath!!)
                //end of line
                mDrawPath = CustomPath(color, mBrushSize)
            }
        }
        view?.invalidateCanvas()
    }

    /**
     * Next method change brush size proportionally for any screen
     */
    fun setBrushSize(newSize: Float){
        //adapting size for any screen
        if (newSize <= 50)
        mBrushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            Resources.getSystem().displayMetrics)
        //change size of brush
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    override fun getBrushSize(): Int {
        return mBrushSize.toInt()
    }

    /**
     * Next two methods remove lines
     */
    //Remove last one
    fun removeLastLine(){
        if (mPaths.size != 0) {
            mPaths.removeAt(mPaths.size - 1)
            view?.invalidateCanvas()
        }
    }
    //Remove all lines
    fun removeAllLines(){
        if (mPaths.size != 0) {
            mPaths.clear()
            view?.invalidateCanvas()
        }
    }

    /**
     * Next method change color to selected by user
     */
    override fun setBrushColorFromInt(mColor: Int) {
        //parse needed color
        color = mColor
        //change color
        mDrawPaint!!.color = color
    }

    override fun setBrushColorFromString(mColor: String) {
        //parse needed color
        color = Color.parseColor(mColor)
        //change color
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