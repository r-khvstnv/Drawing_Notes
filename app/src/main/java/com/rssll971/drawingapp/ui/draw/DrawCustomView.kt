/************************************************
 * Created by Ruslan Khvastunov                 *
 * r.khvastunov@gmail.com                       *
 * Copyright (c) 2022                           *
 * All rights reserved.                         *
 *                                              *
 ************************************************/

package com.rssll971.drawingapp.ui.draw

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.rssll971.drawingapp.R
import com.rssll971.drawingapp.di.DaggerViewComponent
import com.rssll971.drawingapp.di.ViewModule
import javax.inject.Inject


class DrawCustomView(context: Context, attrs: AttributeSet) :
    View(context, attrs), DrawContract.DrawView {
    @Inject
    lateinit var presenter: DrawPresenter
    /**parent layout of view*/
    private var frameLayout: FrameLayout? = null


    init {
        injector()
        presenter.attach(this)
        presenter.initTouchDetector(context = context)
        presenter.setupDrawingOptions()
    }

    private fun injector(){
        val injectorDraw = DaggerViewComponent.builder().viewModule(ViewModule()).build()
        injectorDraw.inject(this)
    }

    override fun invalidateCanvas() {
        invalidate()
    }

    override fun scaleView(scaleFactor: Float) {
        frameLayout!!.scaleX = scaleFactor
        frameLayout!!.scaleY = scaleFactor
    }

    override fun moveView(x: Float, y: Float) {
        frameLayout!!.x += x
        frameLayout!!.y += y
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        presenter.onViewSizeChanged(w, h)
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        presenter.onDrawRequest(canvas = canvas)
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (frameLayout == null)
            frameLayout = this.rootView.findViewById(R.id.fl_container)

        presenter.touchEventProduced(event)
        return true
    }
}