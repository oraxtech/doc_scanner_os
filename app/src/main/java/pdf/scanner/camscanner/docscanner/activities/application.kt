package pdf.scanner.camscanner.docscanner.activities

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader


class DocApplication :  Application(){
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}