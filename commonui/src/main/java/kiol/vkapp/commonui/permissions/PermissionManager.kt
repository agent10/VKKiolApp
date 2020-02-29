package kiol.vkapp.commonui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import timber.log.Timber

typealias PermissionsCallback = (isGranted: Boolean) -> Unit

class PermissionManager(private val context: Context, private val permissions: List<String>) {

    companion object {
        const val PermissionRequestCode = 42
    }

    private var block: PermissionsCallback = {}

    fun checkPermissions(fragment: Fragment, block: PermissionsCallback) {
        if (isGranted()) {
            Timber.d("Permissions already granted")

            block.invoke(true)
        } else {
            this.block = block
            Timber.d("Start permissions request")
            fragment.requestPermissions(
                permissions.toTypedArray(), PermissionRequestCode
            )
        }
    }

    fun invokePermissionRequest(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PermissionRequestCode) {
            Timber.d("onRequestPermissionsResult, grantResults: $grantResults, permissions: $permissions")
            val isGranted = isGranted()
            block.invoke(isGranted)
            block = {}
        }
    }

    private fun isGranted(): Boolean {
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }

        return true
    }
}