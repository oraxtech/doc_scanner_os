package pdf.scanner.camscanner.docscanner.core

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class ExternalStorageUtil {

   companion object {
        fun createNewFolder(parentFolder: DocumentFile?, folderName: String) : DocumentFile? {
           // Check if a folder with the same name already exists
           val existingFolder = parentFolder?.findFile(folderName)
           var newFile : DocumentFile? = null
           Log.e("Existing Folder",existingFolder?.exists().toString())
           if (existingFolder != null && existingFolder.isDirectory) {

                newFile = existingFolder.createFile("application/pdf", getFileName())

           } else {
               val newFolder = parentFolder?.createDirectory(folderName)

               if (newFolder != null && newFolder.exists()) {
                   newFile = newFolder.createFile("application/pdf", getFileName())
               } else {
             //      Failed to create new folder
               }
           }
           return newFile
       }

       private fun getFileName(): String {
           val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
           return "PDF_$timestamp.pdf"
       }
        fun getOutputFile(context: Context, folderName: String): File? {
            val root: File = File("${Environment.getExternalStorageDirectory()}/Documents/$folderName/")
            Log.e("root",root.toString())
            val rootPublic: File = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)}",folderName)
            Log.e("root Public",rootPublic.toString())

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

        fun getImageFile(folderName: String,fileName: String): File? {
            val root: File = File("${Environment.getExternalStorageDirectory()}/Documents/$folderName/")

            var isFolderCreated: Boolean = true

            if (!root.exists()) {
                isFolderCreated = root.mkdir()
            }

            return if (isFolderCreated) {
//                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
//                val fileName = "JPG_$timestamp.jpg"
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