package pdf.scanner.camscanner.docscanner.core

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.itextpdf.layout.element.Image

import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class MediaUtil {

    companion object {
        private fun getBitmapFromUri(context: Context, imageUri: Uri): Bitmap {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(
                        context.contentResolver,
                        imageUri
                    )
                )
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }

        }

        private fun getUriFromAbsolutePath(context: Context, absolutePath: String?): Uri? {
            val file = absolutePath?.let { File(it) }
            return file?.let {
                FileProvider.getUriForFile(
                    context, context.packageName + ".provider",
                    it
                )
            }
        }

        fun getBitmapCacheUri(context: Context, bitmap: Bitmap): Uri? {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val imageFileName = "cache_$timestamp.jpg"
            val file = File(context.cacheDir, imageFileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()

            return getUriFromAbsolutePath(context, file.absolutePath)
        }

        fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
            var uri: Uri? = null

            try {
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()

                val stream = FileOutputStream("$cachePath/image.png")
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                val file = File(cachePath, "image.png")

                // Get the content URI using the FileProvider
                uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val authority = context.packageName + ".provider"
                    FileProvider.getUriForFile(context, authority, file)
                } else {
                    Uri.fromFile(file)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return uri
        }

        fun getScaledBitmap(bitmap: Bitmap): Bitmap {
            val maxSize = 1500
            val outHeight: Int
            val outWidth: Int
            val inWidth: Int = bitmap.width
            val inHeight: Int = bitmap.height
            if (inWidth > inHeight) {
                outWidth = maxSize
                outHeight = inHeight * maxSize / inWidth
            } else {
                outHeight = maxSize
                outWidth = inWidth * maxSize / inHeight
            }


            return Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true)
        }

        fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {

            val matrix = Matrix()
            matrix.postRotate(degrees)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        }

        fun compressBitmap(context: Context, uri: Uri?): Bitmap {
            var compressedBitmap: Bitmap
            val bitmap = getBitmapFromUri(context, uri!!)
            val realPathFromURI: String = getRealPathFromURI(context, uri)

            val i: Int = readPictureDegree(realPathFromURI)


            val maxHeight = 2500
            compressedBitmap = if (bitmap.height > maxHeight) {
                val matrix = Matrix()
                matrix.setScale(0.5f, 0.5f);
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)

            } else {
                val matrix = Matrix()
                matrix.setScale(0.9f, 0.9f);
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
            }

            val spBitmap: Bitmap = rotatingImageView(i, compressedBitmap)
            compressedBitmap = spBitmap

            return compressedBitmap
        }

        private fun getRealPathFromURI(context: Context, imageUri: Uri): String {
            var filePath= ""
            try {
                filePath = getFilePathByUri(context, imageUri)!!
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Log.e("filePath",filePath)
            return filePath
        }

        private fun readPictureDegree(path: String): Int {
            var degree = 0
            try {
                val exifInterface = ExifInterface(path)

//                val orientation: Int = exifInterface.getAttributeInt(
//                    ExifInterface.TAG_ORIENTATION,
//                    ExifInterface.ORIENTATION_NORMAL
//                )
                val orientation = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)
                Log.e("Orientation", orientation.toString())
                when (orientation?.let { Integer.parseInt(it) }) {
                    ExifInterface.ORIENTATION_NORMAL -> degree = 90
                    ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
                }
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, e.message!!)
            }

            return degree

        }

        private fun rotatingImageView(angle: Int, srcBitmap: Bitmap): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle.toFloat())

            return Bitmap.createBitmap(
                srcBitmap, 0, 0,
                srcBitmap.width, srcBitmap.height, matrix, true
            )
        }


        private fun getFilePathByUri(context: Context, uri: Uri): String? {
            try {
                if ("content".equals(uri.scheme, ignoreCase = true)) {
                    val sdkVersion = Build.VERSION.SDK_INT
                    return if (sdkVersion >= 19) { // api >= 19
                        getRealPathFromUriAboveApi19(context, uri)
                    } else { // api < 19
                        getRealPathFromUriBelowAPI19(context, uri)
                    }
                } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                    return uri.path
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }


        private fun getRealPathFromUriAboveApi19(context: Context, uri: Uri): String? {
            var filePath: String? = null
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val documentId = DocumentsContract.getDocumentId(uri)
                if (isMediaDocument(uri)) { // MediaProvider
                    val type = documentId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0]
                    val id = documentId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]
                    val selection = MediaStore.Images.Media._ID + "=?"
                    val selectionArgs = arrayOf(id)

                    //
                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    filePath = getDataColumn(context, contentUri, selection, selectionArgs)
                } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(documentId)
                    )
                    filePath = getDataColumn(context, contentUri, null, null)
                } else if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        filePath = "${Environment.getExternalStorageDirectory()}/${split[1]}"
                    }
                } else {
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                // URI of the content type
                filePath = getDataColumn(context, uri, null, null)
            } else if ("file" == uri.scheme) {
                // If the URI of the file type is used, obtain the path of the image.
                filePath = uri.path
            }
            return filePath
        }


        private fun getRealPathFromUriBelowAPI19(context: Context, uri: Uri): String? {
            return getDataColumn(context, uri, null, null)
        }


        private fun getDataColumn(
            context: Context,
            uri: Uri?,
            selection: String?,
            selectionArgs: Array<String>?
        ): String? {
            var path: String? = null
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            var cursor: Cursor? = null
            try {
                cursor =
                    context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex: Int = cursor.getColumnIndexOrThrow(projection[0])
                    path = cursor.getString(columnIndex)
                }
            } catch (e: Exception) {
                cursor?.close()
            }
            return path
        }


        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }


        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }


        fun rotatePoints(
            points: MutableList<Point>,
            angle: Float,
            imageBitmap: Bitmap
        ): MutableList<Point> {
            val rotatedPoints = mutableListOf<Point>()

            val matrix = Matrix()
            matrix.setRotate(
                angle,
                imageBitmap.width.toFloat() / 2,
                imageBitmap.height.toFloat() / 2
            )

            for (point in points) {
                val pointArray = floatArrayOf(point.x.toFloat(), point.y.toFloat())
                matrix.mapPoints(pointArray)
                rotatedPoints.add(Point(pointArray[0].toInt(), pointArray[1].toInt()))
            }
            val documentWidth = imageBitmap.width
            val documentHeight = imageBitmap.height

            val translationX = (documentHeight - documentWidth) / 2
            val translationY = (documentWidth - documentHeight) / 2

            for (point in rotatedPoints) {
                point.offset(translationX, translationY)
            }
            return rotatedPoints
        }

        fun setDefaultPoints(bitmap: Bitmap): MutableList<Point> {
            val defaultCoordinates = mutableListOf<Point>()
            val width = bitmap.width
            val height = bitmap.height
            defaultCoordinates.add(Point(50, 50))
            defaultCoordinates.add(Point(width - 50, 50))
            defaultCoordinates.add(Point(width - 50, height - 50))
            defaultCoordinates.add(Point(50, height - 50))
            return defaultCoordinates
        }

//        fun checkImageStateContainUnfilteredBitmap(imageIndex: Int): Boolean {
//            val currentImageState = IntentServices.getSelectedImagesList()[imageIndex]
//            return currentImageState.unfilteredBitmap != null
//        }

        private fun compressBitmapForPDF(bitmap: Bitmap, quality: Int): Bitmap {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream)
            val compressedBitmap =
                BitmapFactory.decodeStream(ByteArrayInputStream(outputStream.toByteArray()))
            outputStream.close()
            return compressedBitmap
        }

//        fun scaleImageToPage(image: Bitmap, standardPageSize: PDRectangle): Bitmap {
//            val scaleFactor = standardPageSize.width / image.width.toFloat()
//            val targetWidth = standardPageSize.width.toInt()
//            val targetHeight = (image.height * scaleFactor).toInt()
//            return Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true)
//        }

//        fun createPdf(context: Context, bitmaps: List<Bitmap>) {
//            val file = ExternalStorageUtil.getOutputFile(context, "My PDFs/Images to PDF")
//            val fileOutputStream = FileOutputStream(file)
//
//            //  val pageSize = PageSize.A4
//            //   val pdf = PdfDocument()
//            val document = Document(PageSize.A4)
//            val pdfWriter = PdfWriter.getInstance(document, fileOutputStream)
//
//            //  pdfWriter.setFullCompression()
//            pdfWriter.compressionLevel = 9
//            //  pdf.defaultPageSize = PageSize.A4
////            pdf.open()
//            CoroutineScope(Dispatchers.IO).launch {
//                //  try {
//                document.open()
//                // val contentByte = pdfWriter.directContent
//
//                for (i in bitmaps) {
//                    val fileFromBitmap = persistImage(i, "temp")
//
//                    val compressedImageFile = Compressor.compress(context, fileFromBitmap) {
//                        resolution(1700, 2300)
//                        quality(80)
//                        format(Bitmap.CompressFormat.JPEG)
//                        size(2_097_152)
//                    }
//                    val image = Image(compressedImageFile)
//
//                    image.scaleToFit(document.pageSize.width, document.pageSize.height)
//                    val x = (PageSize.A4.width - image.scaledWidth) / 2
//                    val y = (PageSize.A4.height - image.scaledHeight) / 2
//                    image.setAbsolutePosition(x, y)
//                    document.newPage()
//                    document.add(image)
//
//                }
////                } catch (e: Exception) {
////                    e.printStackTrace()
////                } finally {
//                document.close()
//                pdfWriter.close()
//                //  pdf.close()
//                // }
//            }
//        }


        private fun toByteArray(bitmap: Bitmap): ByteArray {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            stream.close()
            return byteArray
        }

        private fun persistImage(bitmap: Bitmap, name: String): File {
            val filesDir: File = Environment.getExternalStorageDirectory()
            val imageFile = File(filesDir, "$name.jpg")
            val os: OutputStream
            try {
                os = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                os.flush()
                os.close()
            } catch (e: java.lang.Exception) {
                Log.e(javaClass.simpleName, "Error writing bitmap", e)
            }
            return imageFile
        }
    }
}

