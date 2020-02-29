package kiol.vkapp.commonui.permissions

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton

class PermissionsSettingsBtn @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {
    init {
        setOnClickListener(NoPermissionBtnListener())
    }
}