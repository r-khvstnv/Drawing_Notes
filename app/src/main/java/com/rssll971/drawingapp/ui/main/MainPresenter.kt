package com.rssll971.drawingapp.ui.main

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathUtils
import androidx.core.view.isVisible
import com.rssll971.drawingapp.R
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI
import java.util.*
import kotlin.io.path.Path

class MainPresenter: MainContract.Presenter {
    private var view: MainContract.MainView? = null
    private var context: Context? = null
    private var job = Job()
    private var scopeForSaving = CoroutineScope(job + Dispatchers.IO)

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
        val directory = Environment.DIRECTORY_PICTURES
        val date = System.currentTimeMillis()
        val fileName = "DN" + date/1000
        val format = ".png"
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                val resolver = context?.contentResolver
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + format)
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                contentValues.put(MediaStore.MediaColumns.DATE_ADDED, date)
                contentValues.put(MediaStore.MediaColumns.DATE_MODIFIED, date)
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
                val imageUri = resolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!
                val openOutputStream = resolver.openOutputStream(Objects.requireNonNull(imageUri))
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, openOutputStream)
                Objects.requireNonNull<OutputStream>(openOutputStream)
                resultPath = File(
                    Environment.getExternalStoragePublicDirectory(directory),
                    fileName + format).absolutePath
            } else{
                val bytes = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                val file = File(
                    Environment.getExternalStoragePublicDirectory(directory),
                    fileName + format)
                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.write(bytes.toByteArray())
                fileOutputStream.close()
                resultPath = file.absolutePath
            }

            if (!resultPath.isNullOrEmpty()){
                if (File(resultPath!!).exists())
                    view?.showShareOption(resultPath!!)
            }
        }.onFailure {
            it.printStackTrace()
        }
    }
}