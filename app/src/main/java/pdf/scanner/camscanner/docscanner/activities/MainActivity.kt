package pdf.scanner.camscanner.docscanner.activities

import android.content.Intent
import android.graphics.Bitmap

import android.net.Uri
import android.os.Build
import android.os.Bundle

import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.itextpdf.kernel.pdf.EncryptionConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.ReaderProperties
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.kernel.utils.PdfMerger
import com.itextpdf.kernel.utils.PdfSplitter

import com.vansuita.pickimage.bundle.PickSetup
import com.vansuita.pickimage.dialog.PickImageDialog
import com.vansuita.pickimage.listeners.IPickClick

import pdf.scanner.camscanner.docscanner.R
import pdf.scanner.camscanner.docscanner.core.*
import pdf.scanner.camscanner.docscanner.databinding.ActivityMainBinding
import java.io.FileOutputStream
import java.io.InputStream


class MainActivity : AppCompatActivity() {
    private var floatingButton: FloatingActionButton? = null
    private var pickImageDialog: PickImageDialog? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setRecyclerView()
        //  Security.insertProviderAt(BouncyCastleProvider(), 1)
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

    private fun pickPDFFromDevice() {
        val pdfIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        pdfFromDevice.launch(pdfIntent)
    }


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
                            val listOfInputStream = createInputStreamList(listOfUri)
                            mergePDF(listOfInputStream)
                        } else {
                            Toast.makeText(this, "Must Select Two PDF Files", Toast.LENGTH_SHORT)
                                .show()
                        }
                        listOfUri.trimToSize()
                        listOfUri.clear()
                    }
                    SelectedTool.EXTRACT -> {
                        val listOfUri: ArrayList<Uri> = ArrayList()
                        if (it.data?.clipData != null) {
                            // extractPagesFromPdf(it.data?.clipData?.getItemAt(0)!!.uri, listOf(1,3,6))
                            Toast.makeText(this, "Select only one pdf", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            listOfUri.add(it.data!!.data!!)
                            val listOfInputStream = createInputStreamList(listOfUri)
                            extractPagesFromPdf(listOfInputStream, listOf(1, 4, 5))
                            listOfUri.trimToSize()
                            listOfUri.clear()
                        }
                    }
                    SelectedTool.REORDER -> {
                        val listOfUri: ArrayList<Uri> = ArrayList()
                        if (it.data?.clipData != null) {
                            Toast.makeText(this, "Select only one pdf", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            listOfUri.add(it.data!!.data!!)
                            val listOfInputStream = createInputStreamList(listOfUri)
                            reorderPdfPages(listOfInputStream, listOf(1, 4, 5))
                            listOfUri.trimToSize()
                            listOfUri.clear()
                        }
                    }
                    SelectedTool.PROTECT -> {
                        if (it.data?.clipData != null) {
                            Toast.makeText(this, "Select only one pdf", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            val listOfUri: ArrayList<Uri> = ArrayList()
                            listOfUri.add(it.data!!.data!!)
                            val listOfInputStream = createInputStreamList(listOfUri)
                            protectPdf(listOfInputStream)
                            listOfUri.trimToSize()
                            listOfUri.clear()
                        }
                    }
//                    else -> {
//
//                    }
//                }
//                Log.e(TAG, it.data?.clipData?.getItemAt(0)?.uri.toString())
                    else -> {}
                }
            }
        }

    private fun createInputStreamList(listOfUri: MutableList<Uri>): List<InputStream> {
        val listOfInputStream = ArrayList<InputStream>()
        for (uri in listOfUri) {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                listOfInputStream.add(inputStream)
            }
        }

        return listOfInputStream
    }

    private fun mergePDF(listOfInputStreamPdf: List<InputStream>) {
        try {
            val file = ExternalStorageUtil.getOutputFile(this, "My PDFs/Merged")
            val fileOutputStream = FileOutputStream(file)

            val pdfDocument =
                PdfDocument(
                    PdfReader(
                        listOfInputStreamPdf[0],
                        ReaderProperties().setPassword("owner".toByteArray())
                    ), PdfWriter(fileOutputStream)
                )
            val merger = PdfMerger(pdfDocument)

            for (i in listOfInputStreamPdf.indices) {
                val pdfNumber = i + 1
                if (pdfNumber in listOfInputStreamPdf.indices) {
                    val mergePdfDocument = PdfDocument(
                        PdfReader(
                            listOfInputStreamPdf[pdfNumber],
                            ReaderProperties().setPassword("owner".toByteArray())
                        )
                    )
                    merger.merge(mergePdfDocument, 1, mergePdfDocument.numberOfPages)
                    mergePdfDocument.close()
                    listOfInputStreamPdf[pdfNumber].close()
                }
            }

            pdfDocument.close()
            fileOutputStream.close()

            // Show a message or perform any other action after successful merging
            Toast.makeText(this, "Merged using itext 7", Toast.LENGTH_SHORT).show()
            Log.d("PDF Merge", "PDF files merged successfully!");
        } catch (e: java.lang.Exception) {
            e.printStackTrace();
            // Handle the error appropriately
        }
    }


    private fun extractPagesFromPdf(
        listOfInputStreamPdf: List<InputStream>,
        pageNumbers: List<Int>
    ) {
        val file = ExternalStorageUtil.getOutputFile(this, "My PDFs/Merged")
        val fileOutputStream = FileOutputStream(file)

        val reader = PdfReader(listOfInputStreamPdf[0])

        // Create a PdfDocument object for the input PDF
        val document = PdfDocument(reader)

        // Create a PdfWriter object for the output PDF
        val writer = PdfWriter(fileOutputStream)

        // Create a new PdfDocument object for the output PDF
        val outputDocument = PdfDocument(writer)

        try {
            // Iterate through the page numbers and copy them to the output document
            for (pageNum in pageNumbers) {
                if (pageNum >= 1 && pageNum <= document.numberOfPages) {
                    val page = document.getPage(pageNum)
                    outputDocument.addPage(page.copyTo(outputDocument))
                }
            }
            listOfInputStreamPdf[0].close()
            Toast.makeText(this, "Extracted using itext 7", Toast.LENGTH_SHORT).show()

        } finally {
            // Close all the PdfDocument objects to release resources
            outputDocument.close()
            document.close()
        }

    }

    private fun reorderPdfPages(listOfInputStreamPdf: List<InputStream>, newPageOrder: List<Int>) {
        val file = ExternalStorageUtil.getOutputFile(this, "My PDFs/Reordered")
        val fileOutputStream = FileOutputStream(file)

        val reader = PdfReader(listOfInputStreamPdf[0])

        // Create a PdfDocument object for the input PDF
        val document = PdfDocument(reader)

        // Create a PdfWriter object for the output PDF
        val writer = PdfWriter(fileOutputStream)

        // Create a new PdfDocument object for the output PDF
        val outputDocument = PdfDocument(writer)
        outputDocument.initializeOutlines()
        val totalPages = document.numberOfPages

        if (totalPages >= 2) {
            val tempPages = (1..totalPages).toList().shuffled()

            document.copyPagesTo(tempPages, outputDocument)
        }

        document.close()
        outputDocument.close()
        fileOutputStream.close()
        listOfInputStreamPdf[0].close()
        Toast.makeText(this, "Reordered using itext 7", Toast.LENGTH_SHORT).show()

    }

    private fun protectPdf(listOfInputStreamPdf: List<InputStream>) {
        val file = ExternalStorageUtil.getOutputFile(this, "My PDFs/Protected")
        val fileOutputStream = FileOutputStream(file)
        val pdfReader = PdfReader(listOfInputStreamPdf[0])
        val writerProperties = WriterProperties()
        writerProperties.setStandardEncryption(
            "user".toByteArray(),
            "Owner".toByteArray(),
            EncryptionConstants.ALLOW_PRINTING,
            EncryptionConstants.ENCRYPTION_AES_128
        )
        val pdfWriter = PdfWriter(fileOutputStream, writerProperties)
        val pdfDocument = PdfDocument(pdfReader, pdfWriter)
        pdfDocument.close()
        fileOutputStream.close()
        listOfInputStreamPdf[0].close()
        Toast.makeText(this, "Protected using itext 7", Toast.LENGTH_SHORT).show()

    }

}


