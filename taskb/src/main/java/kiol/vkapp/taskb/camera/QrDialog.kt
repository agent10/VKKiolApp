package kiol.vkapp.taskb.camera

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.androidbrowserhelper.trusted.TwaLauncher
import kiol.vkapp.taskb.R

class QrDialog : BottomSheetDialogFragment() {

    companion object {
        private const val QR_BODY = "qrbody"

        fun create(qrBody: String): QrDialog {
            return QrDialog().apply {
                arguments = Bundle().apply {
                    putString(QR_BODY, qrBody)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.qr_dialog_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val qrBody = arguments?.getString(QR_BODY, "Unknown").orEmpty()


        val qrTitleTv = view.findViewById<TextView>(R.id.title)
        val qrBodyTv = view.findViewById<TextView>(R.id.qrBody)
        val qrHttpBtn = view.findViewById<TextView>(R.id.httpOpenBtn)
        qrHttpBtn.setOnClickListener {
            TwaLauncher(requireContext()).launch(Uri.parse(qrBody))
            dismissAllowingStateLoss()
        }

        qrBodyTv.text = qrBody

        if (qrBody.isUrl()) {
            qrTitleTv.text = "Внешня ссыль"
        } else {
            qrHttpBtn.visibility = View.GONE
        }
    }

    private fun String.isUrl() = URLUtil.isHttpUrl(this) || URLUtil.isHttpsUrl(this)

}