package kiol.vkapp.taskb.camera.ui

import android.R
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageButton

class CheckableImageButton(context: Context?, attrs: AttributeSet?) :
    AppCompatImageButton(context, attrs), Checkable {

    private var mChecked = false
    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) View.mergeDrawableStates(drawableState,
            CHECKED_STATE_SET
        )
        return drawableState
    }

    override fun toggle() {
        isChecked = !mChecked
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun setChecked(checked: Boolean) {
        if (mChecked == checked) return
        mChecked = checked
        refreshDrawableState()
    }

    override fun performClick(): Boolean {
        toggle()
        return super.performClick()
    }

    companion object {
        private val CHECKED_STATE_SET = intArrayOf(R.attr.state_checked)
    }
}