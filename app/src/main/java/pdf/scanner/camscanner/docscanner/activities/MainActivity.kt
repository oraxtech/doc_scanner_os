package pdf.scanner.camscanner.docscanner.activities

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.nfc.Tag
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfDocument
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfWriter
import com.vansuita.pickimage.bundle.PickSetup
import com.vansuita.pickimage.dialog.PickImageDialog
import com.vansuita.pickimage.listeners.IPickClick
import pdf.scanner.camscanner.docscanner.R
import pdf.scanner.camscanner.docscanner.core.*
import pdf.scanner.camscanner.docscanner.databinding.ActivityMainBinding
import java.io.FileOutputStream
import java.io.IOException
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


    private fun extractPagesFromPdf(uri: Uri) {
        var inputPdf: InputStream? = null
        try {
            val file = ExternalStorageUtil.getOutputFile(this, "My PDFs/Extracted")
            val fileOutputStream = FileOutputStream(file)
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
}

