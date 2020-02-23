package kiol.vkapp.taskb.camera

import android.graphics.Matrix
import android.graphics.RectF
import android.util.Size
import android.view.Surface
import android.view.TextureView

fun TextureView.configureTransform(rotation: Int, previewSize: Size) {
    val matrix = Matrix()
    val viewRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    val centerX = viewRect.centerX()
    val centerY = viewRect.centerY()

    if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
        with(matrix) {
            postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        }
    } else if (Surface.ROTATION_180 == rotation) {
        matrix.postRotate(180f, centerX, centerY)
    }

    val bufferRatio = previewSize.height / previewSize.width.toFloat()

    val scaledWidth: Float
    val scaledHeight: Float
    if (viewRect.width() > viewRect.height()) {
        scaledHeight = viewRect.width()
        scaledWidth = viewRect.width() * bufferRatio
    } else {
        scaledHeight = viewRect.height()
        scaledWidth = viewRect.height() * bufferRatio
    }

    val xScale = scaledWidth / viewRect.width()
    val yScale = scaledHeight / viewRect.height()

    matrix.preScale(xScale, yScale, centerX, centerY)

    matrix.mapRect(viewRect)

    val scale = if (viewRect.width() * height.toFloat() > width.toFloat() * viewRect.height()) {
        height.toFloat() / viewRect.height()
    } else {
        width.toFloat() / viewRect.width()
    }

    matrix.preScale(scale, scale, centerX, centerY)
    setTransform(matrix)
}