package pdf.scanner.camscanner.docscanner.core

import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri

data class ImageState(
    val id: Int,
    var bitmap: Bitmap? = null,
    var editedBitmap: Bitmap? = null,
    var imageUri: Uri? = null
    )
