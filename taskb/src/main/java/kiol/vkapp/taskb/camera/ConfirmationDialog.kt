package kiol.vkapp.taskb

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment

/**
 * Shows OK/Cancel confirmation dialog about camera permission.
 */
class ConfirmationDialog : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(activity)
            .setMessage(getString(R.string.permission_rational))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                parentFragment?.requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    1
                )
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                parentFragment?.activity?.finish()
            }
            .create()
}