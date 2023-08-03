package pdf.scanner.camscanner.docscanner.activities

import android.app.Application
import javax.xml.parsers.SAXParserFactory


class DocApplication :  Application(){
    override fun onCreate() {
        super.onCreate()
     //   PDFBoxResourceLoader.init(applicationContext)
    }
}