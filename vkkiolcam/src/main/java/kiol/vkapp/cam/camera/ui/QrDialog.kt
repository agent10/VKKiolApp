package kiol.vkapp.cam.camera.ui

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.androidbrowserhelper.trusted.TwaLauncher
import kiol.vkapp.cam.R
import kiol.vkapp.cam.camera.CameraContainerFragment

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
            qrTitleTv.text = getText(R.string.qr_link)
        } else {
            qrTitleTv.text = getText(R.string.qr_text)
            qrHttpBtn.visibility = View.GONE
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (parentFragment as CameraContainerFragment).setEnableQrCallback(true)
    }

    private fun String.isUrl() = URLUtil.isHttpUrl(this) || URLUtil.isHttpsUrl(this)

}