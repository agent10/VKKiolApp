package kiol.vkapp.cam.camera.ui

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.AttributeSet
import android.view.View

class VignetteDrawable : ShapeDrawable(RectShape()) {
    init {
        shaderFactory = object : ShapeDrawable.ShaderFactory() {
            override fun resize(width: Int, height: Int): Shader {
                return LinearGradient(
                    width / 2f,
                    0f,
                    width / 2f,
                    height.toFloat(),
                    intArrayOf(0x77000000, Color.TRANSPARENT, Color.TRANSPARENT, 0x88000000.toInt()),
                    floatArrayOf(0f, 0.15f, 0.65f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        }
    }
}

class Vignette @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    init {
        background = VignetteDrawable()
    }
}