package com.jiadanni.launcher4d.viewutil

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Region
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.util.Tool
import kotlin.math.roundToInt
import kotlin.math.sqrt

class GroupDrawable(context: Context, item: Item, iconSize: Int) : Drawable() {

    private lateinit var icons: Array<Drawable?>
    private var iconsCount: Int = 0
    private lateinit var paintInnerCircle: Paint
    private lateinit var paintOuterCircle: Paint
    private lateinit var paintIcon: Paint
    private var needAnimate = false
    private var needAnimateScale = false
    private var scaleFactor = 1f
    private var iconSize: Float = 0f
    private var padding: Float = 0f

    // For group of 3 icons (1st row extra padding)
    private var padding31: Float = 0f

    // For group of 3 icons (2nd row extra padding)
    private var padding32: Float = 0f
    private var outline: Int = 0
    private var iconSizeDiv2: Int = 0
    private var iconSizeDiv4: Int = 0

    init {
        val size = Tool.dp2px(iconSize.toFloat())
        val iconsArray = arrayOfNulls<Drawable>(4)

        init(iconsArray, item.items!!.size, size.toFloat())
        for (i in 0 until 4.coerceAtMost(item.items!!.size)) {
            val temp = item.items!![i]
            val app = temp?.let { Setup.appLoader().findItemApp(it) }

            if (app == null) {
                Log.d(
                    this::class.java.name,
                    "Item ${item.label} has a null app at index $i (Intent: ${temp?.intent ?: "Item is NULL"})"
                )
                icons[i] = ColorDrawable(Color.TRANSPARENT)
            } else {
                icons[i] = app.icon
            }
        }
    }

    override fun getIntrinsicHeight(): Int = iconSize.roundToInt()

    override fun getIntrinsicWidth(): Int = iconSize.roundToInt()

    private fun init(iconsArray: Array<Drawable?>, count: Int, size: Float) {
        icons = iconsArray
        iconsCount = count
        iconSize = size
        iconSizeDiv2 = (iconSize / 2f).roundToInt()
        iconSizeDiv4 = (iconSize / 4f).roundToInt()
        padding = iconSize / 25f
        val b = iconSize / 2f + 2 * padding
        padding31 = b * PADDING31_KOEF
        padding32 = b * (PADDING32_KOEF - PADDING31_KOEF)

        paintInnerCircle = Paint().apply {
            color = Setup.appSettings().desktopFolderColor
            alpha = 150
            isAntiAlias = true
        }

        outline = Tool.dp2px(2)
        paintOuterCircle = Paint().apply {
            color = Setup.appSettings().desktopFolderColor
            isAntiAlias = true
            flags = Paint.ANTI_ALIAS_FLAG
            style = Paint.Style.STROKE
            strokeWidth = outline.toFloat()
        }

        paintIcon = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
    }

    fun popUp() {
        needAnimate = true
        needAnimateScale = true
        invalidateSelf()
    }

    fun popBack() {
        needAnimate = false
        needAnimateScale = false
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        canvas.save()

        scaleFactor = if (needAnimateScale) {
            Tool.clampFloat(scaleFactor - 0.09f, 0.5f, 1f)
        } else {
            Tool.clampFloat(scaleFactor + 0.09f, 0.5f, 1f)
        }

        canvas.scale(scaleFactor, scaleFactor, iconSize / 2f, iconSize / 2f)

        val clip = Path().apply {
            addCircle(iconSize / 2f, iconSize / 2f, iconSize / 2f - outline, Path.Direction.CW)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            canvas.clipPath(clip, Region.Op.REPLACE)
        } else {
            canvas.clipPath(clip)
        }

        canvas.drawCircle(iconSize / 2f, iconSize / 2f, iconSize / 2f - outline, paintInnerCircle)

        when {
            iconsCount > 3 -> {
                icons[0]?.let { drawIcon(canvas, it, padding, padding, iconSizeDiv2 - padding, iconSizeDiv2 - padding, paintIcon) }
                icons[1]?.let { drawIcon(canvas, it, iconSizeDiv2 + padding, padding, iconSize - padding, iconSizeDiv2 - padding, paintIcon) }
                icons[2]?.let { drawIcon(canvas, it, padding, iconSizeDiv2 + padding, iconSizeDiv2 - padding, iconSize - padding, paintIcon) }
                icons[3]?.let { drawIcon(canvas, it, iconSizeDiv2 + padding, iconSizeDiv2 + padding, iconSize - padding, iconSize - padding, paintIcon) }
            }
            iconsCount > 2 -> {
                icons[0]?.let { drawIcon(canvas, it, padding, padding + padding31, iconSizeDiv2 - padding, iconSizeDiv2 - padding + padding31, paintIcon) }
                icons[1]?.let { drawIcon(canvas, it, iconSizeDiv2 + padding, padding + padding31, iconSize - padding, iconSizeDiv2 - padding + padding31, paintIcon) }
                icons[2]?.let { drawIcon(canvas, it, padding + iconSizeDiv4, iconSizeDiv2 + padding + padding32, iconSizeDiv4 + iconSizeDiv2 - padding, iconSize - padding + padding32, paintIcon) }
            }
            else -> {
                icons[0]?.let { drawIcon(canvas, it, padding, padding + iconSizeDiv4, iconSizeDiv2 - padding, iconSizeDiv4 + iconSizeDiv2 - padding, paintIcon) }
                icons[1]?.let { drawIcon(canvas, it, iconSizeDiv2 + padding, padding + iconSizeDiv4, iconSize - padding, iconSizeDiv4 + iconSizeDiv2 - padding, paintIcon) }
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            canvas.clipRect(0f, 0f, iconSize, iconSize, Region.Op.REPLACE)
        }

        canvas.drawCircle(iconSize / 2f, iconSize / 2f, iconSize / 2f - outline, paintOuterCircle)
        canvas.restore()

        if (needAnimate) {
            paintIcon.alpha = Tool.clampInt(paintIcon.alpha - 25, 0, 255)
            invalidateSelf()
        } else if (paintIcon.alpha != 255) {
            paintIcon.alpha = Tool.clampInt(paintIcon.alpha + 25, 0, 255)
            invalidateSelf()
        }
    }

    private fun drawIcon(canvas: Canvas, icon: Drawable, l: Float, t: Float, r: Float, b: Float, paint: Paint) {
        icon.setBounds(l.toInt(), t.toInt(), r.toInt(), b.toInt())
        icon.isFilterBitmap = true
        icon.alpha = paint.alpha
        icon.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        // Not implemented
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // Not implemented
    }

    override fun getOpacity(): Int = PixelFormat.TRANSPARENT

    companion object {
        private val PADDING31_KOEF = 1f - sqrt(3f) / 2f
        private val PADDING32_KOEF = (sqrt(3f) - 1f) / (2f * sqrt(3f))
    }
}
