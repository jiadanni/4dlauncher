package com.jiadanni.launcher4d.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.jiadanni.launcher4d.activity.HomeActivity
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.util.Tool
import `in`.championswimmer.sfg.lib.SimpleFingerGestures
import kotlin.math.max
import kotlin.math.min

open class CellContainer @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null
) : ViewGroup(context, attr) {

    private var animateBackground = false
    private val bgPaint = Paint(1)
    private var blockTouch = false
    private var cachedOutlineBitmap: Bitmap? = null
    private var _cellHeight = 0
    private var _cellSpanH = 0
    private var _cellSpanV = 0
    private var _cellWidth = 0
    private var cells: Array<Array<Rect>>? = null
    private val currentOutlineCoordinate = Point(-1, -1)
    private var down: Long = 0
    private var gestures: SimpleFingerGestures? = null
    private var hideGrid = true
    private val paint = Paint(1)
    private var occupied: Array<BooleanArray>? = null
    private val outlinePaint = Paint(1)
    private var peekDirection: PeekDirection? = null
    private var peekDownTime: Long = -1
    private val preCoordinate = Point(-1, -1)
    private val startCoordinate = Point()
    private val tempRect = Rect()

    enum class DragState {
        CurrentNotOccupied, OutOffRange, ItemViewNotFound, CurrentOccupied
    }

    class LayoutParams : ViewGroup.LayoutParams {
        var x: Int = 0
            private set
        var xSpan: Int = 1
            private set
        var y: Int = 0
            private set
        var ySpan: Int = 1
            private set

        fun setX(v: Int) {
            x = v
        }

        fun setY(v: Int) {
            y = v
        }

        fun setXSpan(v: Int) {
            xSpan = v
        }

        fun setYSpan(v: Int) {
            ySpan = v
        }

        constructor(w: Int, h: Int, x: Int, y: Int) : super(w, h) {
            this.x = x
            this.y = y
        }

        constructor(w: Int, h: Int, x: Int, y: Int, xSpan: Int, ySpan: Int) : super(w, h) {
            this.x = x
            this.y = y
            this.xSpan = xSpan
            this.ySpan = ySpan
        }

        constructor(w: Int, h: Int) : super(w, h)
    }

    enum class PeekDirection {
        UP, LEFT, RIGHT, DOWN
    }

    val cellWidth: Int
        get() = this._cellWidth

    val cellHeight: Int
        get() = this._cellHeight

    val cellSpanV: Int
        get() = this._cellSpanV

    val cellSpanH: Int
        get() = this._cellSpanH

    fun setBlockTouch(v: Boolean) {
        blockTouch = v
    }

    fun setGestures(v: SimpleFingerGestures?) {
        gestures = v
    }

    val allCells: List<View>
        get() {
            val views = ArrayList<View>()
            val childCount = childCount
            for (i in 0 until childCount) {
                views.add(getChildAt(i))
            }
            return views
        }

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = Color.WHITE
        paint.alpha = 0
        bgPaint.style = Paint.Style.FILL
        bgPaint.color = Color.WHITE
        bgPaint.alpha = 0
        outlinePaint.color = Color.WHITE
        outlinePaint.alpha = 0
        init()
    }

    fun setGridSize(x: Int, y: Int) {
        _cellSpanV = y
        _cellSpanH = x

        occupied = Array(cellSpanH) { BooleanArray(cellSpanV) }
        for (i in 0 until cellSpanH) {
            for (j in 0 until cellSpanV) {
                occupied!![i][j] = false
            }
        }

        requestLayout()
    }

    fun setHideGrid(hideGrid: Boolean) {
        this.hideGrid = hideGrid
        invalidate()
    }

    fun resetOccupiedSpace() {
        if (cellSpanH > 0 && cellSpanV > 0) {
            occupied = Array(cellSpanH) { BooleanArray(cellSpanV) }
        }
    }

    override fun removeAllViews() {
        resetOccupiedSpace()
        super.removeAllViews()
    }

    fun projectImageOutlineAt(newCoordinate: Point, bitmap: Bitmap?) {
        cachedOutlineBitmap = bitmap
        if (currentOutlineCoordinate != newCoordinate) {
            outlinePaint.alpha = 0
        }
        currentOutlineCoordinate.set(newCoordinate.x, newCoordinate.y)
        invalidate()
    }

    private fun drawCachedOutlineBitmap(canvas: Canvas, cell: Rect) {
        cachedOutlineBitmap?.let { bitmap ->
            val centerX = cell.centerX().toFloat()
            val centerY = cell.centerY().toFloat()
            canvas.drawBitmap(bitmap, centerX - (bitmap.width / 2), centerY - (bitmap.height / 2), outlinePaint)
        }
    }

    fun clearCachedOutlineBitmap() {
        outlinePaint.alpha = 0
        cachedOutlineBitmap = null
        invalidate()
    }

    fun peekItemAndSwap(event: DragEvent, coordinate: Point): DragState {
        return peekItemAndSwap(event.x.toInt(), event.y.toInt(), coordinate)
    }

    fun peekItemAndSwap(x: Int, y: Int, coordinate: Point): DragState {
        touchPosToCoordinate(coordinate, x, y, 1, 1, false, false)
        if (coordinate.x != -1 && coordinate.y != -1) {
            if (!preCoordinate.equals(coordinate.x, coordinate.y)) {
                peekDownTime = -1
            }
            if (peekDownTime == -1L) {
                peekDirection = getPeekDirectionFromCoordinate(startCoordinate, coordinate)
                peekDownTime = System.currentTimeMillis()
                preCoordinate.set(coordinate.x, coordinate.y)
            }
            return if (occupied!![coordinate.x][coordinate.y]) {
                DragState.CurrentOccupied
            } else {
                DragState.CurrentNotOccupied
            }
        }
        return DragState.OutOffRange
    }

    private fun getPeekDirectionFromCoordinate(from: Point, to: Point): PeekDirection? {
        return when {
            from.y - to.y > 0 -> PeekDirection.UP
            from.y - to.y < 0 -> PeekDirection.DOWN
            from.x - to.x > 0 -> PeekDirection.LEFT
            from.x - to.x < 0 -> PeekDirection.RIGHT
            else -> null
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (blockTouch) {
            return super.onTouchEvent(event)
        }
        try {
            gestures?.onTouch(this, event)
        } catch (e: Exception) {
            e.printStackTrace()
            return super.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (blockTouch) return true
        return super.onInterceptTouchEvent(ev)
    }

    fun init() {
        setWillNotDraw(false)
    }

    fun animateBackgroundShow() {
        animateBackground = true
        invalidate()
    }

    fun animateBackgroundHide() {
        animateBackground = false
        invalidate()
    }

    fun findFreeSpace(): Point? {
        val currentOccupied = occupied ?: return null
        for (y in 0 until currentOccupied[0].size) {
            for (x in currentOccupied.indices) {
                if (!currentOccupied[x][y]) {
                    return Point(x, y)
                }
            }
        }
        return null
    }

    fun findFreeSpace(spanX: Int, spanY: Int): Point? {
        val currentOccupied = occupied ?: return null
        for (y in 0 until currentOccupied[0].size) {
            for (x in currentOccupied.indices) {
                if (!currentOccupied[x][y] && !checkOccupied(Point(x, y), spanX, spanY)) {
                    return Point(x, y)
                }
            }
        }
        return null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        if (cells == null) return

        val s = 7f
        for (x in 0 until cellSpanH) {
            for (y in 0 until cellSpanV) {
                if (x >= cells!!.size || y >= cells!![0].size) continue

                val cell = cells!![x][y]

                canvas.save()
                canvas.rotate(45f, cell.left.toFloat(), cell.top.toFloat())
                canvas.drawRect(cell.left - s, cell.top - s, cell.left + s, cell.top + s, paint)
                canvas.restore()

                canvas.save()
                canvas.rotate(45f, cell.left.toFloat(), cell.bottom.toFloat())
                canvas.drawRect(cell.left - s, cell.bottom - s, cell.left + s, cell.bottom + s, paint)
                canvas.restore()

                canvas.save()
                canvas.rotate(45f, cell.right.toFloat(), cell.top.toFloat())
                canvas.drawRect(cell.right - s, cell.top - s, cell.right + s, cell.top + s, paint)
                canvas.restore()

                canvas.save()
                canvas.rotate(45f, cell.right.toFloat(), cell.bottom.toFloat())
                canvas.drawRect(cell.right - s, cell.bottom - s, cell.right + s, cell.bottom + s, paint)
                canvas.restore()
            }
        }

        // Animating alpha and drawing projected image
        Tool.getLauncher(context)?.let { homeActivity ->
            if (homeActivity.itemOptionView.dragExceedThreshold && currentOutlineCoordinate.x != -1 && currentOutlineCoordinate.y != -1) {
                if (outlinePaint.alpha != 160) {
                    outlinePaint.alpha = min(outlinePaint.alpha + 20, 160)
                }
                drawCachedOutlineBitmap(canvas, cells!![currentOutlineCoordinate.x][currentOutlineCoordinate.y])

                if (outlinePaint.alpha <= 160) {
                    invalidate()
                }
            }
        }

        // Animating alpha
        if (hideGrid && paint.alpha != 0) {
            paint.alpha = max(paint.alpha - 20, 0)
            invalidate()
        } else if (!hideGrid && paint.alpha != 255) {
            paint.alpha = min(paint.alpha + 20, 255)
            invalidate()
        }

        // Animating alpha
        if (!animateBackground && bgPaint.alpha != 0) {
            bgPaint.alpha = max(bgPaint.alpha - 10, 0)
            invalidate()
        } else if (animateBackground && bgPaint.alpha != 100) {
            bgPaint.alpha = min(bgPaint.alpha + 10, 100)
            invalidate()
        }
    }

    override fun addView(view: View) {
        val lp = view.layoutParams as LayoutParams
        setOccupied(true, lp)
        super.addView(view)
    }

    override fun removeView(view: View) {
        val lp = view.layoutParams as LayoutParams
        setOccupied(false, lp)
        super.removeView(view)
    }

    fun addViewToGrid(view: View, x: Int, y: Int, xSpan: Int, ySpan: Int) {
        view.layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT, x, y, xSpan, ySpan)
        addView(view)
    }

    fun addViewToGrid(view: View) {
        addView(view)
    }

    fun setOccupied(b: Boolean, lp: LayoutParams) {
        val xSpan = lp.x + lp.xSpan
        for (x in lp.x until xSpan) {
            val ySpan = lp.y + lp.ySpan
            for (y in lp.y until ySpan) {
                occupied!![x][y] = b
            }
        }
    }

    fun checkOccupied(start: Point, spanX: Int, spanY: Int): Boolean {
        val currentOccupied = occupied ?: return true

        if (start.x + spanX > currentOccupied.size) {
            return true
        }
        if (start.y + spanY > currentOccupied[0].size) {
            return true
        }

        for (i in start.y until start.y + spanY) {
            for (x in start.x until start.x + spanX) {
                if (currentOccupied[x][i]) {
                    return true
                }
            }
        }
        return false
    }

    fun coordinateToChildView(pos: Point?): View? {
        pos ?: return null

        for (i in 0 until childCount) {
            val lp = getChildAt(i).layoutParams as LayoutParams
            if (pos.x >= lp.x && pos.y >= lp.y && pos.x < lp.x + lp.xSpan && pos.y < lp.y + lp.ySpan) {
                return getChildAt(i)
            }
        }
        return null
    }

    fun coordinateToLayoutParams(mX: Int, mY: Int, xSpan: Int, ySpan: Int): LayoutParams? {
        val pos = Point()
        touchPosToCoordinate(pos, mX, mY, xSpan, ySpan, true)
        return if (!pos.equals(-1, -1)) {
            LayoutParams(WRAP_CONTENT, WRAP_CONTENT, pos.x, pos.y, xSpan, ySpan)
        } else {
            null
        }
    }

    fun touchPosToCoordinate(coordinate: Point, mX: Int, mY: Int, xSpan: Int, ySpan: Int, checkAvailability: Boolean) {
        touchPosToCoordinate(coordinate, mX, mY, xSpan, ySpan, checkAvailability, false)
    }

    fun touchPosToCoordinate(
        coordinate: Point,
        mX: Int,
        mY: Int,
        xSpan: Int,
        ySpan: Int,
        checkAvailability: Boolean,
        checkBoundary: Boolean
    ) {
        if (cells == null) {
            coordinate.set(-1, -1)
            return
        }

        var adjustedX = mX - (xSpan - 1) * cellWidth / 2f
        var adjustedY = mY - (ySpan - 1) * cellHeight / 2f

        var x = 0
        while (x < cellSpanH) {
            var y = 0
            while (y < cellSpanV) {
                val cell = cells!![x][y]
                if (adjustedY >= cell.top && adjustedY <= cell.bottom && adjustedX >= cell.left && adjustedX <= cell.right) {
                    var finalX = x
                    var finalY = y

                    if (checkAvailability) {
                        if (occupied!![x][y]) {
                            coordinate.set(-1, -1)
                            return
                        }

                        var dx = x + xSpan - 1
                        var dy = y + ySpan - 1

                        if (dx >= cellSpanH - 1) {
                            dx = cellSpanH - 1
                            finalX = dx + 1 - xSpan
                        }
                        if (dy >= cellSpanV - 1) {
                            dy = cellSpanV - 1
                            finalY = dy + 1 - ySpan
                        }

                        for (x2 in finalX until finalX + xSpan) {
                            for (y2 in finalY until finalY + ySpan) {
                                if (occupied!![x2][y2]) {
                                    coordinate.set(-1, -1)
                                    return
                                }
                            }
                        }
                    }

                    if (checkBoundary) {
                        val offsetCell = Rect(cell)
                        val dp2 = Tool.dp2px(6)
                        offsetCell.inset(dp2, dp2)
                        if (adjustedY >= offsetCell.top && adjustedY <= offsetCell.bottom &&
                            adjustedX >= offsetCell.left && adjustedX <= offsetCell.right) {
                            coordinate.set(-1, -1)
                            return
                        }
                    }

                    coordinate.set(finalX, finalY)
                    return
                }
                y++
            }
            x++
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l - paddingLeft - paddingRight
        val height = b - t - paddingTop - paddingBottom

        if (_cellSpanH == 0) {
            _cellSpanH = 1
        }
        if (_cellSpanV == 0) {
            _cellSpanV = 1
        }

        _cellWidth = width / cellSpanH
        _cellHeight = height / cellSpanV
        initCellInfo(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)

        val count = childCount
        if (cells != null) {
            for (i in 0 until count) {
                val child = getChildAt(i)
                if (child.visibility != View.GONE) {
                    val lp = child.layoutParams as LayoutParams
                    child.measure(
                        MeasureSpec.makeMeasureSpec(lp.xSpan * cellWidth, View.MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(lp.ySpan * cellHeight, View.MeasureSpec.EXACTLY)
                    )

                    val upRect = cells!![lp.x][lp.y]
                    var downRect = tempRect

                    if (lp.x + lp.xSpan - 1 < cellSpanH && lp.y + lp.ySpan - 1 < cellSpanV) {
                        downRect = cells!![lp.x + lp.xSpan - 1][lp.y + lp.ySpan - 1]
                    }

                    when {
                        lp.xSpan == 1 && lp.ySpan == 1 -> {
                            child.layout(upRect.left, upRect.top, upRect.right, upRect.bottom)
                        }
                        lp.xSpan > 1 && lp.ySpan > 1 -> {
                            child.layout(upRect.left, upRect.top, downRect.right, downRect.bottom)
                        }
                        lp.xSpan > 1 -> {
                            child.layout(upRect.left, upRect.top, downRect.right, upRect.bottom)
                        }
                        lp.ySpan > 1 -> {
                            child.layout(upRect.left, upRect.top, upRect.right, downRect.bottom)
                        }
                    }
                }
            }
        }
    }

    private fun initCellInfo(l: Int, t: Int, r: Int, b: Int) {
        cells = Array(cellSpanH) { Array(cellSpanV) { Rect() } }

        var curLeft = l
        var curTop = t
        var curRight = l + cellWidth
        var curBottom = t + cellHeight

        for (i in 0 until cellSpanH) {
            if (i != 0) {
                curLeft += cellWidth
                curRight += cellWidth
            }

            for (j in 0 until cellSpanV) {
                if (j != 0) {
                    curTop += cellHeight
                    curBottom += cellHeight
                }

                val rect = Rect(curLeft, curTop, curRight, curBottom)
                cells!![i][j] = rect
            }

            curTop = t
            curBottom = t + cellHeight
        }
    }

    companion object {
        const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
