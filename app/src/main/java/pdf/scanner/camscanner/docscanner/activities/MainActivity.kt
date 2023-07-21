package pdf.scanner.camscanner.docscanner.activities

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfImportedPage
import com.itextpdf.text.pdf.PdfReader
import com.vansuita.pickimage.bundle.PickSetup
import com.vansuita.pickimage.dialog.PickImageDialog
import com.vansuita.pickimage.listeners.IPickClick
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pdf.scanner.camscanner.docscanner.BuildConfig
import pdf.scanner.camscanner.docscanner.R
import pdf.scanner.camscanner.docscanner.core.*
import pdf.scanner.camscanner.docscanner.databinding.ActivityMainBinding
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.Security


class MainActivity : AppCompatActivity() {
    private var floatingButton: FloatingActionButton? = null
    private var pickImageDialog: PickImageDialog? = null
    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setRecyclerView()
        Security.insertProviderAt(BouncyCastleProvider(), 1)
        floatingButton = findViewById(R.id.main_activity_floating_button)
        floatingButton!!.setOnClickListener {
            onFloatingButtonPressed()
        }
    }

    private fun setRecyclerView() {
        val adapter = ToolsFragmentRecyclerViewAdapter(
            applicationContext,
            Constants.getToolsFragmentItemsList(applicationContext)
        ) {
            IntentServices.setSelectedTool(it)
//          if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)  {
//                val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
//
//                startActivity(
//                    Intent(
//                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
//
//            )
//            )
//        }
            val permissionsList = listOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (checkPermissions(permissionsList, ::pickPDFFromDevice)) {
                pickPDFFromDevice()
            }
        }
        binding.fragmentToolsRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL, false
        )
        binding.fragmentToolsRecyclerView.adapter = adapter
    }

    private fun onFloatingButtonPressed() {
        val permissionsList = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (checkPermissions(permissionsList, ::showPickImageDialog)) {
            showPickImageDialog()
        }

    }

    private fun checkPermissions(permissionsList: List<String>, callBack: () -> Unit): Boolean {

        return PermissionServices.checkPermissions(
            applicationContext,
            permissionsList,
            packageName
        ) { callBack.invoke() }
    }

    private fun showPickImageDialog() {
        pickImageDialog =
            PickImageDialog.build(
                PickSetup().setTitle(getString(R.string.text_select))
                    .setCameraButtonText(getString(R.string.text_camera))
                    .setGalleryButtonText(
                        getString(R.string.text_gallery)
                    ).setCancelText(getString(R.string.text_cancel))
            ).setOnClick(object : IPickClick {
                override fun onGalleryClick() {
                    getScannedImageFromGallery()
                }

                override fun onCameraClick() {
                    Toast.makeText(
                        this@MainActivity,
                        "You can only select from gallery",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

            }).show(this)

    }


    private fun getScannedImageFromGallery() {
        imageFromGallery.launch("image/*")
    }

    private val imageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) {
            if (it != null && it.isNotEmpty()) {
                val listOfSelectedImages = ArrayList<ImageState>()
                it.forEachIndexed { index, uri ->
                    var compressedBitmap: Bitmap
                    this.applicationContext?.let { applicationContext ->
                        compressedBitmap = MediaUtil.compressBitmap(
                            applicationContext,
                            uri
                        )
                        listOfSelectedImages.add(
                            ImageState(
                                id = index,
                                bitmap = compressedBitmap,
                                imageUri = uri,
                            ),
                        )
                    }
                }
                IntentServices.setSelectedImagesList(listOfSelectedImages)
                //  viewPager.adapter?.notifyDataSetChanged()
                val intent = Intent(this@MainActivity, PDFConvertor::class.java)
                startActivity(intent)
                pickImageDialog!!.dismiss()
            }
        }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun pickPDFFromDevice() {
        val pdfIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        pdfFromDevice.launch(pdfIntent)
       // documentPicker.launch(arrayOf("application/pdf"))

    }
//    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){
//        if(it.isNotEmpty()){
//            Log.e("it","Running")
//        }
//    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val pdfFromDevice =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                when (IntentServices.getSelectTool()) {
                    SelectedTool.MERGE -> {
                        val listOfUri: ArrayList<Uri> = ArrayList()
                        if (it.data?.clipData != null) {
                            val clipData = it.data?.clipData
                            for (i in 0 until clipData?.itemCount!!) {
                                val uri = clipData.getItemAt(i).uri
                                listOfUri.add(uri)
                            }
                            mergePDF(listOfUri)
                        } else {
                            Toast.makeText(this, "Must Select Two PDF Files", Toast.LENGTH_SHORT)
                                .show()
                        }

                    }
                    SelectedTool.EXTRACT -> {
                        if (it.data?.clipData != null) {
                            extractPagesFromPdf(it.data?.clipData?.getItemAt(0)!!.uri)
                        }else{
                            extractPagesFromPdf(it.data!!.data!!)
                        }
                    }SelectedTool.REORDER -> {
                    if (it.data?.clipData != null) {
                        reorderPdfPages(it.data?.clipData?.getItemAt(0)!!.uri)
                    }else{
                        reorderPdfPages(it.data!!.data!!)
                    }
                    }SelectedTool.PROTECT -> {
                    if (it.data?.clipData != null) {
                        protectPdf(it.data?.clipData?.getItemAt(0)!!.uri,"user","owner")
                    }else{
                        protectPdf(it.data!!.data!!,"user","owner")
                    }
                    }
                    else -> {

                    }
                }
                Log.e(TAG, it.data?.clipData?.getItemAt(0)?.uri.toString())
            }
        }

    private fun mergePDF(listOfUri: MutableList<Uri>) {
        try {
            val file = ExternalStorageUtil.getOutputFile(this, "My PDFs/Merged")
            val fileOutputStream = FileOutputStream(file)
            val document = Document()
            val copy = PdfCopy(document, fileOutputStream)

            document.open();

            // Read and merge each selected PDF from content URI
            for (uri in listOfUri) {
                // InputStream inputStream = getContentResolver().openInputStream(uri);
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val reader = PdfReader(inputStream);
                val totalPages = reader.numberOfPages;
                for (page in 1..totalPages) {
                    copy.addPage(copy.getImportedPage(reader, page))
                }
                reader.close();
                inputStream?.close();
            }

            // Close the output document
            document.close();

            // Show a message or perform any other action after successful merging
            Toast.makeText(this, "PDF files merged successfully!", Toast.LENGTH_SHORT).show()
            Log.d("PDF Merge", "PDF files merged successfully!");
        } catch (e: java.lang.Exception) {
            e.printStackTrace();
            // Handle the error appropriately
        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun extractPagesFromPdf(uri: Uri) {
        var inputPdf: InputStream? = null
        try {
            val file = ExternalStorageUtil.getOutputFile(this, "My PDFs/Extracted")
            val fileOutputStream = FileOutputStream(file)
            generateThumbnails(uri)
            inputPdf = contentResolver.openInputStream(uri)
            inputPdf?.let { inputStream ->
                val reader = PdfReader(inputStream)

                var document: Document? = null
                var copy: PdfCopy? = null

                try {
                    document = Document()
                    copy = PdfCopy(document, fileOutputStream)
                    document.open()
                    val totalPages = reader.numberOfPages
                    for (pageNumber in 1..totalPages step 2) {
                        if (pageNumber in 1..reader.numberOfPages) {
                            val page = copy.getImportedPage(reader, pageNumber)
                            copy.addPage(page)
                        }
                    }
                } catch (e: DocumentException) {
                    e.printStackTrace()
                    // Handle the error appropriately
                } finally {
                    copy?.close()
                    document?.close()
                }

                reader.close()
            }

            // Show a message or perform any other action after successful extraction
            Toast.makeText(this, "Pages extracted successfully!", Toast.LENGTH_SHORT).show()
            Log.d("PDF Extract", "Pages extracted successfully!")
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle the error appropriately
        } finally {
            inputPdf?.close()
        }
    }

    private fun reorderPdfPages(uri: Uri) {
        var inputPdf: InputStream? = null
        var document: Document? = null
        var copy: PdfCopy? = null

        try {
            val file = ExternalStorageUtil.getOutputFile(this, "My PDFs/Reordered")
            val fileOutputStream = FileOutputStream(file)
            inputPdf = contentResolver.openInputStream(uri)
            inputPdf?.let { inputStream ->
                val reader = PdfReader(inputStream,"owner".toByteArray())
                val totalPages = reader.numberOfPages

                if (totalPages >= 2) {
                    val newPageOrder = (1..totalPages).toList().shuffled()

                    document = Document()
                    copy = PdfCopy(document, fileOutputStream)
                    document!!.open()

                    for (newPageIndex in newPageOrder) {
                        if (newPageIndex in 1..totalPages) {
                            val page = copy!!.getImportedPage(reader, newPageIndex)
                            copy!!.addPage(page)
                        }
                    }

                    // Show a message or perform any other action after successful reordering
                    Toast.makeText(this, "Pages reordered successfully", Toast.LENGTH_SHORT).show()
                    Log.d("PDF Reorder", "Pages reordered successfully!")
                } else {
                    // Handle the case when the PDF contains fewer than 2 pages
                    Toast.makeText(
                        this,
                        "The PDF should contain at least 2 pages for reordering.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("PDF Reorder", "The PDF should contain at least 2 pages for reordering.")
                }

                reader.close()
            }
        } catch (e: DocumentException) {
            e.printStackTrace()
            // Handle the error appropriately
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle the error appropriately
        } finally {
            copy?.close()
            document?.close()
            inputPdf?.close()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun generateThumbnails(pdfUri: Uri) {
        var renderer: PdfRenderer? = null
        var fileDescriptor: ParcelFileDescriptor? = null

        try {
           // val pdfFile = File(pdfUri)
             fileDescriptor = this.contentResolver.openFileDescriptor(pdfUri, "r")

            if (fileDescriptor != null) {
                renderer = PdfRenderer(fileDescriptor)

                for (pageNumber in 0 until renderer.pageCount) {
                    val page = renderer.openPage(pageNumber)

                    // Scale the thumbnail image as needed
                    val scale = 0.5f
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    // Generate thumbnail filename (e.g., page_1_thumbnail.jpg)
                    val thumbnailFileName = "page_${pageNumber + 1}_thumbnail.jpg"
                    val file = ExternalStorageUtil.getImageFile("My PDFs/thumbnails",thumbnailFileName)
                    val fileOutputStream = FileOutputStream(file)
                    // Save the thumbnail image to the output directory
                 //   val thumbnailFile = File(outputDirectory, thumbnailFileName)
                    fileOutputStream.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    }

                    page.close()
                }

                // Show a message or perform any other action after successful thumbnail generation
                Log.d("PDF Thumbnails", "Thumbnails generated successfully!")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle the error appropriately
        } finally {
            renderer?.close()
            fileDescriptor?.close()
        }
    }

//    fun protectPdf(inputPdfPath: String, userPassword: String, ownerPassword: String) {
//        val reader = PdfReader(inputPdfPath)
//        val outputStream = FileOutputStream(inputPdfPath)
//        val document = Document()
//       // val writerPropertiesManager = WriterP
//        // Set the encryption options
//        val copy  = PdfCopy(document, outputStream)
//
//        copy.setEncryption(userPassword.toByteArray(), ownerPassword.toByteArray(), PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_256)
//         document.open()
//
//        reader.close()
//        outputStream.close()
//    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun protectPdf(pdfUri: Uri, userPassword: String, ownerPassword: String) {
       // var renderer: PdfRenderer? = null
        var inputPdf: InputStream? = null

       // var fileDescriptor: ParcelFileDescriptor? = null

        try {
            inputPdf = contentResolver.openInputStream(pdfUri)
            inputPdf?.let { inputStream ->
                val reader = PdfReader(inputStream)
                val totalPages = reader.numberOfPages

                val document = Document()
               // val outputStream = FileOutputStream(outputPdfPath)
                val file = ExternalStorageUtil.getOutputFile(this, "My PDFs/Protected")
                val fileOutputStream = FileOutputStream(file)
                val copy = PdfCopy(document, fileOutputStream)

                // Set the encryption options
                copy.setEncryption(userPassword.toByteArray(), ownerPassword.toByteArray(), PdfCopy.ALLOW_SCREENREADERS, PdfCopy.ENCRYPTION_AES_256)

                document.open()

                // Copy pages from the original PDF to the protected PDF
                for (pageNumber in 1 until totalPages) {
                  //  val page = renderer!!.openPage(pageNumber)
                    val importedPage : PdfImportedPage = copy.getImportedPage(reader,pageNumber)
                    copy.addPage(importedPage)
                 //   page.close()
                }

                document.close()
                fileOutputStream.close()
                Toast.makeText(this, "PDF Protected successfully", Toast.LENGTH_SHORT).show()
                // Show a message or perform any other action after successful protection
                // ...
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle the error appropriately
        } finally {
//            renderer?.close()
//            fileDescriptor?.close()
            inputPdf?.close()
        }
    }
}

