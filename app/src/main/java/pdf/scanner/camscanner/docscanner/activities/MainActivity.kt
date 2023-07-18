package pdf.scanner.camscanner.docscanner.activities

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vansuita.pickimage.bundle.PickSetup
import com.vansuita.pickimage.dialog.PickImageDialog
import com.vansuita.pickimage.listeners.IPickClick
import pdf.scanner.camscanner.docscanner.R
import pdf.scanner.camscanner.docscanner.core.ImageState
import pdf.scanner.camscanner.docscanner.core.IntentServices
import pdf.scanner.camscanner.docscanner.core.MediaUtil
import pdf.scanner.camscanner.docscanner.core.PermissionServices

class MainActivity : AppCompatActivity() {
    private var floatingButton: FloatingActionButton? = null
    private var pickImageDialog: PickImageDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        floatingButton = findViewById(R.id.main_activity_floating_button)
        floatingButton!!.setOnClickListener {
            onFloatingButtonPressed()
        }
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
            ).setOnClick(object : IPickClick{
                override fun onGalleryClick() {
                    getScannedImageFromGallery()
                }

                override fun onCameraClick() {
                    Toast.makeText(this@MainActivity, "You can only select from gallery", Toast.LENGTH_SHORT)
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
                val intent = Intent(this@MainActivity,PDFConvertor::class.java)
                startActivity(intent)
                pickImageDialog!!.dismiss()
            }
        }

}