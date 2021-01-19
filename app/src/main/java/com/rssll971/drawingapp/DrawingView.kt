package com.rssll971.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View


class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    //VARIABLES FOR DRAWING

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
    //canvas - холст
    private var canvas: Canvas? = null
    //all created paths
    private val myPaths = ArrayList<CustomPath>()

    //SETUP VARIABLES
    //init block where we initialized all of variables
    init {
        //prepare
        setUpDrawing()
    }
    //*****GENERAL FUNCTIONS
    //
    //prepare for drawing function
    private fun setUpDrawing(){
        //create object of paint
        myDrawPaint = Paint()
        //STYLES of drawing implemented inner class
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

    //CREATE BITMAP FUNCTION
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //create bitmap with current width and height, using this config of colors
        myCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        //create canvas with our bitmap
        canvas = Canvas(myCanvasBitmap!!)
    }

    //Parametrized constructor of drawing, which extend Path with parameterized variables
    internal inner class CustomPath(var color: Int,
                                    var brushThickness: Float) : Path() {

    }


    //*****USING BY USER FUNCTIONS
    //
    //START DRAWING FUNCTION (override existing method)
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //implement our canvasBitmap, starting on top-left using our canvasPaint
        canvas?.drawBitmap(myCanvasBitmap!!, 0f, 0f, myCanvasPaint)

        //display all previous paths
        for (path in myPaths){
            //thickness
            myDrawPaint!!.strokeWidth = path.brushThickness
            //color
            myDrawPaint!!.color = path.color
            //draw them
            canvas?.drawPath(path, myDrawPaint!!)
        }

        //SETUP LINE IMPLEMENTING OBJECT OF INNER CLASS
        //thickness
        myDrawPaint!!.strokeWidth = myDrawPath!!.brushThickness
        //color
        myDrawPaint!!.color = myDrawPath!!.color

        //draw our path using existing parameters upper
        canvas?.drawPath(myDrawPath!!, myDrawPaint!!)
    }

    //OVERRIDE onTouchEvent FUNCTION
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //store position onTouch
        val touchX = event?.x
        val touchY = event?.y

        //what should will be executed onTouch
        when(event?.action){
            //press on the screen
            MotionEvent.ACTION_DOWN ->{
                //SETUP PATH
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

        invalidate()
        return true
    }

    //GET CURRENT BRUSH SIZE
    fun getBrushSize(): Float {
        return myBrushSize
    }

    //CHANGE BRUSH SIZE FUNCTION WIT PROPORTIONALITY FOR ANY SCREEN
    fun setBrushSize(newSize: Float){
        //adapting size for any screen
        myBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, resources.displayMetrics)
        //change size of brush
        myDrawPaint!!.strokeWidth = myBrushSize
    }

    //REMOVE LAST LINE FUNCTION
    fun removeLastLine(){
        if (myPaths.size != 0) {
            myPaths.removeAt(myPaths.size - 1)
            invalidate()
        }else{}
    }

    //SELECT COLOR
    fun setColor(myColor: String){
        //parse needed color
        color = Color.parseColor(myColor)
        //change color
        myDrawPaint!!.color = color
    }



}