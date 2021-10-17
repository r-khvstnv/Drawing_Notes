package com.rssll971.drawingapp.ui.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Environment
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.rssll971.drawingapp.R
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainPresenter: MainContract.Presenter {
    private var view: MainContract.MainView? = null
    private var context: Context? = null
    private var job = Job()
    private var scopeForSaving = CoroutineScope(job + Dispatchers.IO)
    private var fileAbsolutePath: String? = null

    override fun attach(view: MainContract.MainView) {
        this.view = view
    }
    override fun getContext(context: Context) {
        this.context = context
    }
    override fun detach() {
        this.view = null
        scopeForSaving.cancel()
    }

    override fun setViewVisibility(v: View, tag: String) {
        val updatedVisibility = if (v.isVisible)
            View.GONE
        else
            View.VISIBLE

        when(tag){
            context?.getString(R.string.st_extra_options) ->
                view?.changeExtraOptionsVisibility(updatedVisibility)
            context?.getString(R.string.st_brush_size) ->
                view?.changeBrushSizeWindowVisibility(updatedVisibility)
        }
    }

    override fun checkStoragePermission(){
        when{
            ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED -> {
                        view?.showGalleryForImage()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MainActivity.GALLERY_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun getBitmapFromView(v: View): Bitmap {
        val bitmap = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundDrawable = v.background
        if (backgroundDrawable != null)
            backgroundDrawable.draw(canvas)
        else
            canvas.drawColor(Color.WHITE)
        v.draw(canvas)
        return bitmap
    }

    fun onSaveBitmapClick(bitmap: Bitmap){
        scopeForSaving.launch { saveBitmapToStorage(bitmap = bitmap) }
    }
    private suspend fun saveBitmapToStorage(bitmap: Bitmap){
        var resultPath: String? = null
        runCatching {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)

            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "DN" + System.currentTimeMillis()/1000 + ".png")
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(bytes.toByteArray())
            fileOutputStream.close()
            resultPath = file.absolutePath
        }.onFailure {
            it.printStackTrace()
        }
        if (!resultPath.isNullOrEmpty())
            view?.showShareOption(resultPath!!)
    }
}