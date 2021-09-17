package com.rssll971.drawingapp.ui.draw

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.rssll971.drawingapp.di.DaggerViewComponent
import com.rssll971.drawingapp.di.ViewModule
import javax.inject.Inject

/**
 * Next Class responsible for custom drawing view
 */
class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs), DrawContract.DrawView {
    @Inject
    lateinit var presenter: DrawPresenter
    /**
     * BLOCK with all vars for drawing
     */
    //path of sth drawing
    private var mDrawPath: CustomPath? = null
    //map of current path
    private var mCanvasBitmap: Bitmap? = null
    //styles of drawing path
    private var mDrawPaint: Paint? = null
    private var myCanvasPaint: Paint? = null

    //thickness of brush
    private var myBrushSize: Float = 0.toFloat()
    //color of drawing (by default)
    private var color = Color.BLACK
    private var colorHex: String = "#121212"

    //canvas - холст
    private var canvas: Canvas? = null
    //all created paths
    private val myPaths = ArrayList<CustomPath>()
    /** BLOCK ENDS**/
    private var isTouchAllowed: Boolean = true

    //SETUP VARIABLES
    //init block where we initialized all of variables
    init {
        injector()
        //prepare
        setUpDrawing()
        presenter.attach(this)
    }

    private fun injector(){
        val injectorDraw = DaggerViewComponent.builder().viewModule(ViewModule()).build()
        injectorDraw.inject(this)
    }
    /**
     * Next method is conflict resolution between draw and move functionality
     *
     * Move action executes when corresponding button is pressed. In this moment,
     * draw action becomes unavailable and it's override methods can't be run
     */
    fun setIsTouchAllowed(boolean: Boolean){
        isTouchAllowed = boolean
    }
    /**
     * Next method prepares for drawing all needed attr vars,
     *                  such as: color, size, stroke and etc.
     */
    private fun setUpDrawing(){
        //object of paint
        mDrawPaint = Paint()
        //styles of drawing implemented inner class
        mDrawPath = CustomPath(color, myBrushSize)
        //color
        mDrawPath!!.color = color
        //type of drawing is line
        mDrawPaint!!.style = Paint.Style.STROKE
        //style of line ends
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND

        //copy graphic bit from one part to another
        myCanvasPaint = Paint(Paint.DITHER_FLAG)
    }


    /**
     * Next method adapt size of custom canvas
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0){
            Handler(Looper.getMainLooper()).postDelayed({
                //create bitmap with current width and height, using this config of colors
                mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                //create canvas with our bitmap
                canvas = Canvas(mCanvasBitmap!!)
            }, 1000)
        }
        else{
            //create bitmap with current width and height, using this config of colors
            mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            //create canvas with our bitmap
            canvas = Canvas(mCanvasBitmap!!)
        }
    }

    /**
     * In next class using parametrized constructor of drawing, which extend my Path
     */
    internal inner class CustomPath(var color: Int,
                                    var brushThickness: Float) : Path()


    /**
     * Main role of next method is create new users line
     *
     * More deeply below
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //implement our canvasBitmap, starting on top-left using our canvasPaint
        canvas?.drawBitmap(mCanvasBitmap!!, 0f, 0f, myCanvasPaint)

        /**
         * Next loop make visible all previous lines
         */
        for (path in myPaths){
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

    /**
     * Next method takes all parameters for drawing line,
     *         such as path (using x,y), color, thickness and further actions which should execute
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        /** Next statement is crutch for conflict described upper*/
        if (isTouchAllowed){
            //store position onTouch
            val touchX = event?.x
            val touchY = event?.y

            //what should will be executed onTouch
            when(event?.action){
                //press on the screen
                MotionEvent.ACTION_DOWN ->{
                    //setup path
                    mDrawPath!!.color = color
                    mDrawPath!!.brushThickness = myBrushSize

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
                    myPaths.add(mDrawPath!!)
                    //end of line
                    mDrawPath = CustomPath(color, myBrushSize)
                }

                //default
                else -> return false
            }

            //update lines list
            invalidate()

            return true
        }
        return false
    }


    /**
     * Next method change brush size proportionally for any screen
     */
    fun setBrushSize(newSize: Float){
        //adapting size for any screen
        myBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, resources.displayMetrics)
        //change size of brush
        mDrawPaint!!.strokeWidth = myBrushSize
    }


    /**
     * Next two methods remove lines
     */
    //Remove last one
    fun removeLastLine(){
        if (myPaths.size != 0) {
            myPaths.removeAt(myPaths.size - 1)
            invalidate()
        }
    }
    //Remove all lines
    fun removeAllLines(){
        if (myPaths.size != 0) {
            myPaths.clear()
            invalidate()
        }
    }


    /**
     * Next method change color to selected by user
     */
    fun setColor(myColor: String){
        colorHex = "#$myColor"
        //parse needed color
        color = Color.parseColor(myColor)
        //change color
        mDrawPaint!!.color = color

    }
    /**
     * Next method return current color
     */
    fun getCurrentColor(): String{
        return colorHex
    }
}