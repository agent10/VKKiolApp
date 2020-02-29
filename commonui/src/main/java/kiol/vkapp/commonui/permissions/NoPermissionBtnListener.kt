package kiol.vkapp.commonui.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View

class NoPermissionBtnListener : View.OnClickListener {
    override fun onClick(v: View) {
        val context = v.context
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)
        context.startActivity(intent)
    }
}