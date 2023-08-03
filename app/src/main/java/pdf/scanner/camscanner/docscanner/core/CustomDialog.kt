package pdf.scanner.camscanner.docscanner.view.custom_views

import android.app.Dialog
import android.content.Context
import android.opengl.Visibility
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import org.w3c.dom.Text
import pdf.scanner.camscanner.docscanner.R

class CustomDialog(
    context: Context,
    val onYesPressed: () -> Unit,
) : Dialog(context) {

    constructor(
        context: Context,
        onYesPressed: () -> Unit,
        title: String? = null,
        message: String? = null
    ) : this(context, onYesPressed) {
        mTitle = title
        mMessage = message
    }

    constructor(
        context: Context,
        title: String? = null,
        message: String? = null,
        buttonTextOk: CharSequence? = null,
        onYesPressed: () -> Unit,
    ) : this(context, onYesPressed) {
        mTitle = title
        mMessage = message
        mButtonTextOk = buttonTextOk
    }

    init {
        setCancelable(false)

    }

    private var mTitle: String? = null
    private var mMessage: String? = null
    private var buttonYes: Button? = null
    private var buttonNo: Button? = null
    private var mButtonTextOk: CharSequence? = null
    private var titleTextView: TextView? = null
    private var messageTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.custom_dialog)
        buttonYes = findViewById(R.id.custom_dialog_button_yes)
        buttonNo = findViewById(R.id.custom_dialog_button_no)
        titleTextView = findViewById(R.id.custom_dialog_tv_title)
        messageTextView = findViewById(R.id.custom_dialog_tv_body)
        if (mButtonTextOk != null) {
            buttonYes?.text = mButtonTextOk
            buttonNo?.visibility = View.GONE
        }
        if (mTitle != null) {
            titleTextView!!.text = mTitle
        }
        if (mMessage != null) {
            messageTextView!!.text = mMessage
        }
        buttonYes!!.setOnClickListener {
            onYesPressed.invoke()
            dismiss()
        }
        buttonNo!!.setOnClickListener {
            dismiss()
             // Force a crash
        }

    }
}