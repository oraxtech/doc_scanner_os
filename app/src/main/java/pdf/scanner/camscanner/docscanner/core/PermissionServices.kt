package pdf.scanner.camscanner.docscanner.core

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import pdf.scanner.camscanner.docscanner.R
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

object PermissionServices {
    fun checkPermissions(
        context: Context,
        permissionList: List<String>,
        packageName: String?,
        callBack: () -> Unit
    ): Boolean {
        var requiredPermissions = mutableListOf<String>()
        var hasPermissionGranted: Boolean = false

        for (i in permissionList) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    i
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requiredPermissions.add(i)
            }
        }

        if (requiredPermissions.size > 0) {
            Dexter.withContext(context)
                .withPermissions(requiredPermissions)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                        if (report!!.areAllPermissionsGranted()) {
                            hasPermissionGranted = true
                            callBack.invoke()
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {

                            showRationalDialogForPermissions(context, packageName)
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        showRationalDialogForPermissions(context, packageName)
                        hasPermissionGranted = false
                    }

                }).onSameThread().check()
        } else {
            hasPermissionGranted = true
        }
        return hasPermissionGranted
    }

    private fun showRationalDialogForPermissions(context: Context, packageName: String?) {

        AlertDialog.Builder(context)
            .setMessage(
                context.getString(R.string.rational_dialogue_box_text)
            )
            .setPositiveButton(
                context
                    .getString(R.string.rational_dialogue_button_text_go_to_settings)
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(context, intent, null)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton(
                context
                    .getString(R.string.rational_dialogue_button_text_cancel)
            ) { dialog,
                _ ->
                dialog.dismiss()
            }.show()
    }


}