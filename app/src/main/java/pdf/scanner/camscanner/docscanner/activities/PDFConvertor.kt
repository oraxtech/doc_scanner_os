package pdf.scanner.camscanner.docscanner.activities

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.viewpager.widget.ViewPager
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vansuita.pickimage.dialog.PickImageDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pdf.scanner.camscanner.docscanner.R
import pdf.scanner.camscanner.docscanner.core.ImageState
import pdf.scanner.camscanner.docscanner.core.IntentServices
import pdf.scanner.camscanner.docscanner.core.MediaUtil

class PDFConvertor : AppCompatActivity() {
    private lateinit var viewPager: ViewPager
    private var imagesList: ArrayList<ImageState> = ArrayList()
    private lateinit var convertButton: Button
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdfconvertor)
        viewPager = findViewById(R.id.pdf_converter_layout_view_pager)
        convertButton = findViewById(R.id.pdf_converter_layout_button_convert)
        convertButton.setOnClickListener{
            onConvertButtonPressed()
        }
        setUpProgressBar()
        setUpToolBar()
        setView()
    }
    private fun setUpProgressBar() {
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle(getString(R.string.text_progress_bar_please_wait))
        progressDialog.setCanceledOnTouchOutside(false)
    }

    private fun setView() {

        imagesList = IntentServices.getSelectedImagesList()
        setUpViewPager()
    }

    private fun setUpToolBar() {
        val toolBar : androidx.appcompat.widget.Toolbar = findViewById(R.id.activity_pdf_converter_toolbar)
        setSupportActionBar(toolBar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
        }
        toolBar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    private fun setUpViewPager() {

        viewPager.offscreenPageLimit = imagesList.size
        viewPager.adapter =
            PDFConverterActivityViewPagerAdapter(this@PDFConvertor, imagesList)
    }

    private fun onConvertButtonPressed(){
        progressDialog.setMessage(getString(R.string.text_progress_dialog_converting_to_pdf))
        progressDialog.show()
        val bitmapList = ArrayList<Bitmap>()
        for (i in imagesList){
            bitmapList.add(if (i.editedBitmap != null) {
                i.editedBitmap!!
            } else {
                i.bitmap!!
            })
        }
        val handler = Handler(Looper.getMainLooper())
        CoroutineScope(Dispatchers.IO).launch  {
            MediaUtil.createPdf(applicationContext,bitmapList)
            Log.e(ContentValues.TAG, "Converted...")
            progressDialog.dismiss()
            val uri = Uri.parse(
                "${Environment.getExternalStorageDirectory().path}/Documents/My PDFs/"
            )
            val intent = Intent(Intent.ACTION_VIEW)

            intent.setDataAndType(uri, "*/*")
            startActivity(Intent.createChooser(intent, "Open folder"))}
        handler.post{
            Toast.makeText(this, "Converted...", Toast.LENGTH_SHORT).show()
        }
    }
}