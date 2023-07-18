package pdf.scanner.camscanner.docscanner.core

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class ExternalStorageUtil {

   companion object {
        fun getOutputFile(context: Context, folderName: String): File? {
            val root: File = File("${Environment.getExternalStorageDirectory()}/Documents/$folderName/")

            var isFolderCreated: Boolean = true

            if (!root.exists()) {
                isFolderCreated = root.mkdir()
            }

            return if (isFolderCreated) {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val imageFileName = "PDF_$timestamp.pdf"
                File(root, imageFileName)
            } else {
                null
            }
        }

        fun getImageFile(context: Context): File? {
            val root: File = File(context.getExternalFilesDir(null), "Image Doc Scanner")

            var isFolderCreated: Boolean = true

            if (!root.exists()) {
                isFolderCreated = root.mkdir()
            }

            return if (isFolderCreated) {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "JPG_$timestamp.jpg"
                File(root, fileName)
            } else {
                null
            }
        }

        fun getBitmapCacheAbsolutePath(context: Context, bitmap: Bitmap): String {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val imageFileName = "JPG_$timestamp.jpg"
            val file = File(context.cacheDir, imageFileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            outputStream.close()
            return file.absolutePath
        }
    }


}