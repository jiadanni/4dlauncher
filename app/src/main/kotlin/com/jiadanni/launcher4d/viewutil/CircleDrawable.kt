package com.jiadanni.launcher4d.viewutil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import com.jiadanni.launcher4d.util.Tool

class CircleDrawable(
    context: Context,
    icon: Drawable,
    private val iconColor: Int,
    colorBackground: Int,
    alphaBackground: Int
) : Drawable() {

    private val iconSize: Int
    private val iconSizeReal: Int
    private val iconPadding: Int
    private var iconBitmap: Bitmap?
    private var iconToFade: Bitmap? = null
    private val paint: Paint
    private val paint2: Paint

    private val scaleStep = 0.08f
    private var currentScale = 1f
    private var hidingOldIcon = false

    init {
        icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
        iconBitmap = Tool.drawableToBitmap(icon)

        iconPadding = Tool.dp2px(6f)

        iconSizeReal = icon.intrinsicHeight
        iconSize = icon.intrinsicHeight + iconPadding * 2

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorBackground
            alpha = alphaBackground
            style = Paint.Style.FILL
        }

        paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iconColor
            isFilterBitmap = true
        }
    }

    fun setIcon(icon: Drawable) {
        iconToFade = iconBitmap
        hidingOldIcon = true

        icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
        iconBitmap = Tool.drawableToBitmap(icon)
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val centerX = iconSize / 2f
        val centerY = iconSize / 2f
        canvas.drawCircle(centerX, centerY, iconSize / 2f, paint)

        iconToFade?.let { fadeIcon ->
            canvas.save()

            currentScale = if (hidingOldIcon) {
                currentScale - scaleStep
            } else {
                currentScale + scaleStep
            }
            currentScale = Tool.clampFloat(currentScale, 0f, 1f)

            canvas.scale(currentScale, currentScale, centerX, centerY)

            val bitmapToShow = if (hidingOldIcon) fadeIcon else iconBitmap
            val offsetX = centerX - iconSizeReal / 2f
            val offsetY = centerY - iconSizeReal / 2f
            bitmapToShow?.let { canvas.drawBitmap(it, offsetX, offsetY, paint2) }

            canvas.restore()

            if (currentScale == 0f) {
                hidingOldIcon = false
            }

            if (!hidingOldIcon && currentScale == 1f) {
                iconToFade = null
            }

            invalidateSelf()
        } ?: run {
            // No fade animation, just draw the icon
            iconBitmap?.let {
                val offsetX = centerX - iconSizeReal / 2f
                val offsetY = centerY - iconSizeReal / 2f
                canvas.drawBitmap(it, offsetX, offsetY, paint2)
            }
        }
    }

    override fun getIntrinsicWidth(): Int = iconSize

    override fun getIntrinsicHeight(): Int = iconSize

    override fun setAlpha(alpha: Int) {
        // Not implemented
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // Not implemented
    }

    override fun getOpacity(): Int = PixelFormat.TRANSPARENT
}
