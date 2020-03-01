package kiol.vkapp.commondata

import android.content.Context

class SimpleResourceProvider(private val context: Context) {
    fun getString(resId: Int) = context.getString(resId)
}