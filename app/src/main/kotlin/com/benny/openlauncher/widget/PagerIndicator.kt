package com.benny.openlauncher.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.benny.openlauncher.util.Tool

class PagerIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), ViewPager.OnPageChangeListener {

    private var pager: ViewPager? = null
    private val paint = Paint(1)

    private var mode = Mode.DOTS
    private var pad: Float = 0f
    private var dotSize: Float = 0f
    private var previousPage = -1
    private var realPreviousPage: Int = 0

    // current position and offset
    private var scrollOffset: Float = 0f
    private var scrollPosition: Int = 0

    // dot animations
    private var shrinkFactor = 1.0f
    private var expandFactor = 1.5f

    object Mode {
        const val DOTS = 0
        const val LINES = 1
    }

    init {
        setWillNotDraw(false)
        pad = Tool.dp2px(4)
        paint.color = Color.WHITE
        paint.strokeWidth = Tool.dp2px(4)
        paint.isAntiAlias = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        dotSize = (height / 2).toFloat()
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        val currentPager = pager ?: return
        val pageCount = currentPager.adapter?.count ?: return

        when (mode) {
            Mode.DOTS -> {
                val circlesWidth = pageCount * (dotSize + pad * 2)
                canvas.translate(width / 2 - circlesWidth / 2, 0f)

                if (realPreviousPage != currentPager.currentItem) {
                    shrinkFactor = 1f
                    realPreviousPage = currentPager.currentItem
                }

                for (dot in 0 until pageCount) {
                    val stepFactor = 0.05f
                    val smallFactor = 1.0f
                    val largeFactor = 1.5f

                    when {
                        dot == currentPager.currentItem -> {
                            // draw shrinking dot
                            if (previousPage == -1) {
                                previousPage = dot
                            }
                            shrinkFactor = Tool.clampFloat(shrinkFactor + stepFactor, smallFactor, largeFactor)
                            canvas.drawCircle(
                                dotSize / 2 + pad + (dotSize + pad * 2) * dot,
                                (height / 2).toFloat(),
                                shrinkFactor * dotSize / 2,
                                paint
                            )
                            if (shrinkFactor != largeFactor) {
                                invalidate()
                            }
                        }
                        dot != currentPager.currentItem && dot == previousPage -> {
                            // draw expanding dot
                            expandFactor = Tool.clampFloat(expandFactor - stepFactor, smallFactor, largeFactor)
                            canvas.drawCircle(
                                dotSize / 2 + pad + (dotSize + pad * 2) * dot,
                                (height / 2).toFloat(),
                                expandFactor * dotSize / 2,
                                paint
                            )
                            if (expandFactor != smallFactor) {
                                invalidate()
                            } else {
                                expandFactor = 1.5f
                                previousPage = -1
                            }
                        }
                        else -> {
                            // draw normal dot
                            canvas.drawCircle(
                                dotSize / 2 + pad + (dotSize + pad * 2) * dot,
                                (height / 2).toFloat(),
                                dotSize / 2,
                                paint
                            )
                        }
                    }
                }
            }
            Mode.LINES -> {
                val width = (getWidth() / pageCount).toFloat()
                val startX = (scrollPosition + scrollOffset) * width
                val startY = (height / 2).toFloat()

                canvas.drawLine(startX, startY, startX + width, startY, paint)
                if (scrollOffset != 0f) invalidate()
            }
        }
    }

    fun setMode(mode: Int) {
        this.mode = mode
        invalidate()
    }

    fun setViewPager(pager: ViewPager?) {
        if (pager == null && this.pager != null) {
            this.pager?.removeOnPageChangeListener(this)
            this.pager = null
        } else {
            this.pager = pager
            pager?.addOnPageChangeListener(this)
        }
        invalidate()
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        scrollOffset = positionOffset
        scrollPosition = position
        invalidate()
    }

    override fun onPageSelected(position: Int) {
        // nothing
    }

    override fun onPageScrollStateChanged(state: Int) {
        // nothing
    }
}
