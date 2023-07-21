package pdf.scanner.camscanner.docscanner.activities

import android.app.Application


class DocApplication :  Application(){
    override fun onCreate() {
        super.onCreate()
     //   PDFBoxResourceLoader.init(applicationContext)
    }
}