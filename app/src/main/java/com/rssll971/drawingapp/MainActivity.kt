package com.rssll971.drawingapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*

class MainActivity : AppCompatActivity() {
    //variable to store statically text of brush size
    private var textBrushSize: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawing_view.setBrushSize(1.toFloat())
        //thickness of brush
        btn_brush.setOnClickListener {
            //Toast.makeText(this, "1", Toast.LENGTH_LONG).show()
            showBrushSizeDialog()
        }
        //back button
        btn_back.setOnClickListener {
            drawing_view.removeLastLine()
        }

    }

    //TODO COLOR PALETTE
    //TODO REST OF PROJECT

    //DIALOG OF BRUSH SIZE FUNCTION
    private fun showBrushSizeDialog(){
        val brushDialog = Dialog(this)
        //initialize dialog layout
        brushDialog.setContentView(R.layout.dialog_brush_size)
        val brushSize = brushDialog.sb_brush_size
        val displayBrushSize = brushDialog.tv_brush_size_display
        val confirmationBrushSize = brushDialog.btn_confirm_size
        //upload brush size
        displayBrushSize.text = "Size: $textBrushSize"

        //display dialog menu
        brushDialog.show()
        //show current value of size
        brushSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                textBrushSize = i
                displayBrushSize.text = "Size: $textBrushSize"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        //change brush size
        confirmationBrushSize.setOnClickListener {
            //change brush size
            drawing_view.setBrushSize(brushSize.progress.toFloat())
            //turn off brush dialog
            brushDialog.dismiss()
        }
    }

    //CHANGE COLOR OF LINE USING BUTTONS
    //initialize directly in xml file
    //tag takes information of color
    fun colorClicked(view: View){
        //import button
        val button = view as Button
        //import color using tag of button
        val colorTag = button.tag.toString()
        drawing_view.setColor(colorTag)


    }
}