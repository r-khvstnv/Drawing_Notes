package com.rssll971.drawingapp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

/** Used for handling actions with gallery, after permission has been checked*/
class GalleryContract: ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String?): Intent {
        return Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            .apply { type = input }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data.takeIf { resultCode == Activity.RESULT_OK }
    }
}