package com.vkartik.imagecropper

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.OnGestureListener
import android.view.Menu.NONE
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.View.OnTouchListener
import androidx.appcompat.widget.AppCompatImageView

class CropImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), OnTouchListener, OnGestureListener {

    private val boundaryPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f // Adjust the thickness of the boundary as needed
    }

    private val cornerPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val cornerSize = 40f // Adjust the size of the corners as needed

    private var cropRect = Rect(5, 5, width-5, height-5)
    private var activeSide: Side? = null
    private var startX: Float = 0f
    private var startY: Float = 0f

    private enum class Side {
        TOP,
        LEFT,
        RIGHT,
        BOTTOM
    }

    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var myGestureDetector: GestureDetector? = null
    var myMatrix: Matrix? = null
    private var matrixValue: FloatArray? = null
    var zoomMode = 0

    // required Scales
    var presentScale = 1f
    var minimumScale = 1f
    var maximumScale = 4f

    //Dimensions
    var originalWidth = 0f
    var originalHeight = 0f
    var mViewedWidth = 0
    var mViewedHeight = 0
    private var lastPoint = PointF()
    private var startPoint = PointF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE,null)
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        myMatrix = Matrix()
        matrixValue = FloatArray(10)
        imageMatrix = myMatrix
        scaleType = ScaleType.MATRIX
        var myGestureDetector = GestureDetector(context, this)
        setOnTouchListener(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Apply scale factor to the canvas
//        canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)

        // Draw the boundary rectangle
        canvas.drawRect(cropRect, boundaryPaint)
        Log.e("croop", "cropRect $cropRect")

        // Draw the corner triangles
//        drawCornerTriangle(canvas, cropRect.left.toFloat(), cropRect.top.toFloat())
//        drawCornerTriangle(canvas, cropRect.right.toFloat(), cropRect.top.toFloat())
//        drawCornerTriangle(canvas, cropRect.left.toFloat(), cropRect.bottom.toFloat())
//        drawCornerTriangle(canvas, cropRect.right.toFloat(), cropRect.bottom.toFloat())
    }

    private fun drawCornerTriangle(canvas: Canvas, x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(x + cornerSize, y)
        path.lineTo(x, y + cornerSize)
        path.close()
        canvas.drawPath(path, cornerPaint)
    }

//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        scaleGestureDetector?.onTouchEvent(event)
//        if (scaleGestureDetector?.isInProgress == false) {
//            val action = event.action
//            val x = event.x
//            val y = event.y
//
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    activeSide = getActiveSide(x.toInt(), y.toInt())
//                    startX = x
//                    startY = y
//                    previousX = x
//                    previousY = y
//                    isDragging = false
//                }
//
//                MotionEvent.ACTION_MOVE -> {
//                    if (scaleFactor > 1.0f) {
//                        val deltaX = x - previousX
//                        val deltaY = y - previousY
//                        if (!isDragging && (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10)) {
//                            isDragging = true
//                        }
//                        if (isDragging) {
//                            translateImage(deltaX, deltaY)
//                            invalidate()
//                        }
//                    }
//
//                    if (!isDragging && activeSide != null) {
//                        activeSide?.let { side ->
//                            when (side) {
//                                Side.TOP -> {
//                                    cropRect.top = (cropRect.top + (y - startY).toInt()).coerceIn(
//                                        0,
//                                        cropRect.bottom
//                                    )
//                                }
//
//                                Side.LEFT -> {
//                                    cropRect.left = (cropRect.left + (x - startX).toInt()).coerceIn(
//                                        0,
//                                        cropRect.right
//                                    )
//                                }
//
//                                Side.RIGHT -> {
//                                    cropRect.right =
//                                        (cropRect.right + (x - startX).toInt()).coerceIn(
//                                            cropRect.left,
//                                            width
//                                        )
//                                }
//
//                                Side.BOTTOM -> {
//                                    cropRect.bottom =
//                                        (cropRect.bottom + (y - startY).toInt()).coerceIn(
//                                            cropRect.top,
//                                            height
//                                        )
//                                }
//                            }
//                            startX = x
//                            startY = y
//                            invalidate()
//                        }
//                    }
//
//                    previousX = x
//                    previousY = y
//                }
//
//                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                    activeSide = null
//                }
//            }
//        }
//
//        return true
//    }

//    private fun getActiveSide(x: Int, y: Int): Side? {
//        val touchSlop = 10f // Adjust the touch slop as needed for better handling
//        if (Math.abs(y - cropRect.top) <= touchSlop && cropRect.contains(x, y)) {
//            return Side.TOP
//        } else if (Math.abs(x - cropRect.left) <= touchSlop && cropRect.contains(x, y)) {
//            return Side.LEFT
//        } else if (Math.abs(x - cropRect.right) <= touchSlop && cropRect.contains(x, y)) {
//            return Side.RIGHT
//        } else if (Math.abs(y - cropRect.bottom) <= touchSlop && cropRect.contains(x, y)) {
//            return Side.BOTTOM
//        }
//        return null
//    }
//
//    private fun translateImage(deltaX: Float, deltaY: Float) {
//        val scaledDeltaX = deltaX / scaleFactor
//        val scaledDeltaY = deltaY / scaleFactor
//
//        val maxTranslationX = (width / 2f - cropRect.centerX() * scaleFactor) / scaleFactor
//        val maxTranslationY = (height / 2f - cropRect.centerY() * scaleFactor) / scaleFactor
//
//        translationX += scaledDeltaX
//        translationY += scaledDeltaY
//
//        translationX = translationX.coerceIn(-maxTranslationX, maxTranslationX)
//        translationY = translationY.coerceIn(-maxTranslationY, maxTranslationY)
//
//        invalidate()
//    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            zoomMode = 2
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var mScaleFactor = detector.scaleFactor
            val previousScale = mScaleFactor
            presentScale*=mScaleFactor
            if (presentScale > maximumScale) {
                presentScale = maximumScale
                mScaleFactor = maximumScale / previousScale
            }else if (presentScale < minimumScale) {
                presentScale = minimumScale
                mScaleFactor = minimumScale / previousScale
            }
            if (originalWidth * presentScale <= mViewedWidth
                || originalHeight * presentScale <= mViewedHeight
            ) {
                myMatrix!!.postScale(
                    mScaleFactor, mScaleFactor, mViewedWidth / 2.toFloat(),
                    mViewedHeight / 2.toFloat()
                )
            } else {
                myMatrix!!.postScale(
                    mScaleFactor, mScaleFactor,
                    detector.focusX, detector.focusY
                )
            }
            fittedTranslation()
            return true

        }
    }
    private fun putToScreen() {
        presentScale = 1f
        val factor: Float
        val mDrawable = drawable
        if (mDrawable == null || mDrawable.intrinsicWidth == 0 || mDrawable.intrinsicHeight == 0) return
        val mImageWidth = mDrawable.intrinsicWidth
        val mImageHeight = mDrawable.intrinsicHeight
        val factorX = mViewedWidth.toFloat() / mImageWidth.toFloat()
        val factorY = mViewedHeight.toFloat() / mImageHeight.toFloat()
        factor = factorX.coerceAtMost(factorY)
        myMatrix!!.setScale(factor, factor)

        // Centering the image
        var repeatedYSpace = (mViewedHeight.toFloat()
                - factor * mImageHeight.toFloat())
        var repeatedXSpace = (mViewedWidth.toFloat()
                - factor * mImageWidth.toFloat())
        repeatedYSpace /= 2.toFloat()
        repeatedXSpace /= 2.toFloat()
        myMatrix!!.postTranslate(repeatedXSpace, repeatedYSpace)
        originalWidth = mViewedWidth - 2 * repeatedXSpace
        originalHeight = mViewedHeight - 2 * repeatedYSpace
        imageMatrix = myMatrix
    }
    fun fittedTranslation() {
        myMatrix!!.getValues(matrixValue)
        val translationX =
            matrixValue!![Matrix.MTRANS_X]
        val translationY =
            matrixValue!![Matrix.MTRANS_Y]
        val fittedTransX = getFittedTranslation(translationX, mViewedWidth.toFloat(), originalWidth * presentScale)
        val fittedTransY = getFittedTranslation(translationY, mViewedHeight.toFloat(), originalHeight * presentScale)
        if (fittedTransX != 0f || fittedTransY != 0f) myMatrix!!.postTranslate(fittedTransX, fittedTransY)
    }

    private fun getFittedTranslation(mTranslate: Float,vSize: Float, cSize: Float): Float {
        val minimumTranslation: Float
        val maximumTranslation: Float
        if (cSize <= vSize) {
            minimumTranslation = 0f
            maximumTranslation = vSize - cSize
        } else {
            minimumTranslation = vSize - cSize
            maximumTranslation = 0f
        }
        if (mTranslate < minimumTranslation) {
            return -mTranslate + minimumTranslation
        }
        if (mTranslate > maximumTranslation) {
            return -mTranslate + maximumTranslation
        }
        return 0F
    }
    private fun getFixDragTrans(delta: Float, viewedSize: Float, detailSize: Float): Float {
        return if (detailSize <= viewedSize) {
            0F
        } else delta
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        val height = measuredHeight
        cropRect.set(0, 0, width, height)

        mViewedWidth = MeasureSpec.getSize(widthMeasureSpec)
        mViewedHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (presentScale == 1f) {

            // Merged onto the Screen
            putToScreen()
        }
    }
    override fun onTouch(mView: View, mMouseEvent: MotionEvent): Boolean {
        scaleGestureDetector!!.onTouchEvent(mMouseEvent)
        myGestureDetector!!.onTouchEvent(mMouseEvent)
        val currentPoint = PointF(mMouseEvent.x, mMouseEvent.y)

        val mDisplay = this.display
        val mLayoutParams = this.layoutParams
        mLayoutParams.width = mDisplay.width
        mLayoutParams.height = mDisplay.height
        this.layoutParams = mLayoutParams
        when (mMouseEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                lastPoint.set(currentPoint)
                startPoint.set(lastPoint)
                zoomMode = 1
            }
            MotionEvent.ACTION_MOVE -> if (zoomMode == 1) {
                val changeInX = currentPoint.x - lastPoint.x
                val changeInY = currentPoint.y - lastPoint.y
                val fixedTranslationX = getFixDragTrans(changeInX, mViewedWidth.toFloat(), originalWidth * presentScale)
                val fixedTranslationY = getFixDragTrans(changeInY, mViewedHeight.toFloat(), originalHeight * presentScale)
                myMatrix!!.postTranslate(fixedTranslationX, fixedTranslationY)
                fittedTranslation()
                lastPoint[currentPoint.x] = currentPoint.y
            }
            MotionEvent.ACTION_POINTER_UP -> zoomMode = 0
        }
        imageMatrix = myMatrix
        return false
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {
    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return false
    }
}
