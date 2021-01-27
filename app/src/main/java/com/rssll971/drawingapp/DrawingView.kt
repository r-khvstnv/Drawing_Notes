package com.rssll971.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.blue

/**
 * Next Class responsible for custom drawing view
 */
class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    /**
     * BLOCK with all vars for drawing
     */
    //path of sth drawing
    private var myDrawPath: CustomPath? = null
    //map of current path
    private var myCanvasBitmap: Bitmap? = null
    //styles of drawing path
    private var myDrawPaint: Paint? = null
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
        //prepare
        setUpDrawing()
    }

    fun setIsTouchAllowed(boolean: Boolean){
        isTouchAllowed = boolean
    }
    /**
     * Next fun prepares for drawing all needed attr vars,
     *                  such as: color, size, stroke and etc.
     */
    private fun setUpDrawing(){
        //object of paint
        myDrawPaint = Paint()
        //styles of drawing implemented inner class
        myDrawPath = CustomPath(color, myBrushSize)
        //color
        myDrawPath!!.color = color
        //type of drawing is line
        myDrawPaint!!.style = Paint.Style.STROKE
        //style of line ends
        myDrawPaint!!.strokeJoin = Paint.Join.ROUND
        myDrawPaint!!.strokeCap = Paint.Cap.ROUND

        //copy graphic bit from one part to another
        myCanvasPaint = Paint(Paint.DITHER_FLAG)
    }


    /**
     * Next fun create custom canvas implementing bitmap
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //create bitmap with current width and height, using this config of colors
        myCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        //create canvas with our bitmap
        canvas = Canvas(myCanvasBitmap!!)
    }

    /**
     * In next class using parametrized constructor of drawing, which extend my Path
     */
    internal inner class CustomPath(var color: Int,
                                    var brushThickness: Float) : Path() {       }


    /**
     * Main role of next fun is create new users line
     *
     * More deeply below
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //implement our canvasBitmap, starting on top-left using our canvasPaint
        canvas?.drawBitmap(myCanvasBitmap!!, 0f, 0f, myCanvasPaint)

        /**
         * Next loop make visible all previous lines
         */
        for (path in myPaths){
            //thickness
            myDrawPaint!!.strokeWidth = path.brushThickness
            //color
            myDrawPaint!!.color = path.color
            //draw
            canvas?.drawPath(path, myDrawPaint!!)
        }

        /**
         * Next line set current values for line and draw it
         */
        //thickness
        myDrawPaint!!.strokeWidth = myDrawPath!!.brushThickness
        //color
        myDrawPaint!!.color = myDrawPath!!.color
        //draw path
        canvas?.drawPath(myDrawPath!!, myDrawPaint!!)
    }

    /**
     * Next fun takes all parameters for drawing line,
     *         such as path (using x,y), color, thickness and further actions which should execute
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isTouchAllowed){
            //store position onTouch
            val touchX = event?.x
            val touchY = event?.y

            //what should will be executed onTouch
            when(event?.action){

                //press on the screen
                MotionEvent.ACTION_DOWN ->{
                    //setup path
                    myDrawPath!!.color = color
                    myDrawPath!!.brushThickness = myBrushSize

                    //delete any path
                    myDrawPath!!.reset()

                    //start drawing by positions
                    if (touchX != null) {
                        if (touchY != null) {
                            myDrawPath!!.moveTo(touchX, touchY)
                        }
                    }
                }

                //drag over the screen
                MotionEvent.ACTION_MOVE ->{
                    //draw line
                    if (touchX != null) {
                        if (touchY != null) {
                            myDrawPath!!.lineTo(touchX, touchY)
                        }
                    }
                }

                //press gesture
                MotionEvent.ACTION_UP ->{
                    //add created path in array
                    myPaths.add(myDrawPath!!)
                    //end of line
                    myDrawPath = CustomPath(color, myBrushSize)
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
     * Next fun return current brush size
     */
    fun getBrushSize(): Float {
        return myBrushSize
    }

    /**
     * Next fun change brush size proportionally for any screen
     */
    fun setBrushSize(newSize: Float){
        //adapting size for any screen
        myBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, resources.displayMetrics)
        //change size of brush
        myDrawPaint!!.strokeWidth = myBrushSize
    }

    /**
     * Next two fun remove lines
     */
    //Remove last one
    fun removeLastLine(){
        if (myPaths.size != 0) {
            myPaths.removeAt(myPaths.size - 1)
            invalidate()
        }else{}
    }
    //Remove all lines
    fun removeAllLines(){
        if (myPaths.size != 0) {
            myPaths.clear()
            invalidate()
        }else{}
    }

    /**
     * Next fun change color to selected by user
     */
    fun setColor(myColor: String){
        colorHex = "#" + myColor
        //parse needed color
        color = Color.parseColor(myColor)
        //change color
        myDrawPaint!!.color = color

    }
    /**
     * Next fun return current color
     */
    fun getCurrentColor(): String{
        return colorHex
    }
}