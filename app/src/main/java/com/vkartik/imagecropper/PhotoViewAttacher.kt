package com.vkartik.imagecropper

import android.content.Context
import android.graphics.Matrix
import android.graphics.Matrix.ScaleToFit
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.OverScroller
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt


/**
 * The component of [PhotoView] which does the work allowing for zooming, scaling, panning, etc.
 * It is made public in case you need to subclass something other than AppCompatImageView and still
 * gain the functionality that [PhotoView] offers
 */
class PhotoViewAttacher(private val mImageView: ImageView) : OnTouchListener,
    View.OnLayoutChangeListener {
    private var mInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    private var mZoomDuration = DEFAULT_ZOOM_DURATION
    private var mMinScale = DEFAULT_MIN_SCALE
    private var mMidScale = DEFAULT_MID_SCALE
    private var mMaxScale = DEFAULT_MAX_SCALE
    private var mAllowParentInterceptOnEdge = true
    private var mBlockParentIntercept = false

    // Gesture Detectors
    private lateinit var mGestureDetector: GestureDetector
    private lateinit var mScaleDragDetector: CustomGestureDetector

    // These are set so we don't keep allocating them on the heap
    private val mBaseMatrix = Matrix()
    val imageMatrix = Matrix()
    private val mSuppMatrix = Matrix()
    private val mDisplayRect = RectF()
    private val mMatrixValues = FloatArray(9)

    // Listeners
    private var mCurrentFlingRunnable: FlingRunnable? = null
    private var mHorizontalScrollEdge = HORIZONTAL_EDGE_BOTH
    private var mVerticalScrollEdge = VERTICAL_EDGE_BOTH
    private var mBaseRotation: Float = 0f

    private var isZoomEnabled = true
    private var mScaleType = ScaleType.FIT_CENTER
    private val onGestureListener: OnGestureListener = object : OnGestureListener {
        override fun onDrag(dx: Float, dy: Float) {
            if (mScaleDragDetector.isScaling) {
                return  // Do not drag if we are already scaling
            }
            mSuppMatrix.postTranslate(dx, dy)
            checkAndDisplayMatrix()

            /*
             * Here we decide whether to let the ImageView's parent to start taking
             * over the touch event.
             *
             * First we check whether this function is enabled. We never want the
             * parent to take over if we're scaling. We then check the edge we're
             * on, and the direction of the scroll (i.e. if we're pulling against
             * the edge, aka 'overscrolling', let the parent take over).
             */
            val parent = mImageView.parent
            if (mAllowParentInterceptOnEdge && !mScaleDragDetector.isScaling && !mBlockParentIntercept) {
                if (mHorizontalScrollEdge == HORIZONTAL_EDGE_BOTH || mHorizontalScrollEdge == HORIZONTAL_EDGE_LEFT && dx >= 1f || mHorizontalScrollEdge == HORIZONTAL_EDGE_RIGHT && dx <= -1f || mVerticalScrollEdge == VERTICAL_EDGE_TOP && dy >= 1f || mVerticalScrollEdge == VERTICAL_EDGE_BOTTOM && dy <= -1f) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            } else {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
        }

        override fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
            mCurrentFlingRunnable = FlingRunnable(mImageView.context)
            mCurrentFlingRunnable!!.fling(
                getImageViewWidth(mImageView),
                getImageViewHeight(mImageView),
                velocityX.toInt(),
                velocityY.toInt()
            )
            mImageView.post(mCurrentFlingRunnable)
        }

        override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
            onScale(scaleFactor, focusX, focusY, 0f, 0f)
        }

        override fun onScale(
            scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float
        ) {
            if (scale < mMaxScale || scaleFactor < 1f) {
                mSuppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                mSuppMatrix.postTranslate(dx, dy)
                checkAndDisplayMatrix()
            }
        }
    }

    init {
        mImageView.setOnTouchListener(this)
        mImageView.addOnLayoutChangeListener(this)
        if (!mImageView.isInEditMode) {
            mBaseRotation = 0.0f
            // Create Gesture Detectors...
            mScaleDragDetector = CustomGestureDetector(mImageView.context, onGestureListener)
            mGestureDetector = GestureDetector(mImageView.context, SimpleOnGestureListener())
            mGestureDetector.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    val displayRect: RectF? = displayRect
                    val x = e.x
                    val y = e.y

                    if (displayRect?.contains(x, y) == true) {
                        return true
                    }
                    return false
                }

                override fun onDoubleTap(ev: MotionEvent): Boolean {
                    try {
                        val scale: Float = scale
                        val x = ev.x
                        val y = ev.y
                        if (scale < mediumScale) {
                            setScale(mediumScale, x, y, true)
                        } else if (scale >= mediumScale && scale < maximumScale) {
                            setScale(maximumScale, x, y, true)
                        } else {
                            setScale(minimumScale, x, y, true)
                        }
                    } catch (e: ArrayIndexOutOfBoundsException) {
                        // Can sometimes happen when getX() and getY() is called
                    }
                    return true
                }

                override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                    // Wait for the confirmed onDoubleTap() instead
                    return false
                }
            })
        }
    }

    val displayRect: RectF?
        get() {
            checkMatrixBounds()
            return getDisplayRect(drawMatrix)
        }

    private fun setRotationBy(degrees: Float) {
        mSuppMatrix.postRotate(degrees % 360)
        checkAndDisplayMatrix()
    }

    var minimumScale: Float
        get() = mMinScale
        set(minimumScale) {
            Util.checkZoomLevels(minimumScale, mMidScale, mMaxScale)
            mMinScale = minimumScale
        }
    var mediumScale: Float
        get() = mMidScale
        set(mediumScale) {
            Util.checkZoomLevels(mMinScale, mediumScale, mMaxScale)
            mMidScale = mediumScale
        }
    var maximumScale: Float
        get() = mMaxScale
        set(maximumScale) {
            Util.checkZoomLevels(mMinScale, mMidScale, maximumScale)
            mMaxScale = maximumScale
        }
    var scale: Float
        get() = sqrt(
            (getValue(mSuppMatrix, Matrix.MSCALE_X).toDouble().pow(2.0) + getValue(
                mSuppMatrix, Matrix.MSKEW_Y
            ).toDouble().pow(2.0))
        ).toFloat()
        set(scale) {
            setScale(scale, false)
        }
    var scaleType: ScaleType
        get() = mScaleType
        set(scaleType) {
            if (Util.isSupportedScaleType(scaleType) && scaleType != mScaleType) {
                Log.e("pvattach", "scale type $scaleType")
                mScaleType = scaleType
                update()
            }
        }

    override fun onLayoutChange(
        v: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        // Update our base matrix, as the bounds have changed
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            updateBaseMatrix(mImageView.drawable)
        }
    }

    override fun onTouch(v: View, ev: MotionEvent): Boolean {
        var handled = mGestureDetector.onTouchEvent(ev)
        if (!handled && isZoomEnabled && Util.hasDrawable(v as ImageView)) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    val parent = v.getParent()
                    // First, disable the Parent from intercepting the touch
                    // event
                    parent?.requestDisallowInterceptTouchEvent(true)
                    // If we're flinging, and the user presses down, cancel
                    // fling
                    cancelFling()
                }

                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP ->                     // If the user has zoomed less than min scale, zoom back
                    // to min scale
                    if (scale < mMinScale) {
                        displayRect?.let {
                            v.post(
                                AnimatedZoomRunnable(
                                    scale, mMinScale, it.centerX(), it.centerY()
                                )
                            )
                            handled = true
                        }
                    } else if (scale > mMaxScale) {
                        displayRect?.let {
                            v.post(
                                AnimatedZoomRunnable(
                                    scale, mMaxScale, it.centerX(), it.centerY()
                                )
                            )
                            handled = true
                        }
                    }
            }
            // Try the Scale/Drag detector
            val wasScaling: Boolean = mScaleDragDetector.isScaling
            val wasDragging: Boolean = mScaleDragDetector.isDragging
            handled = mScaleDragDetector.onTouchEvent(ev)
            val didntScale = !wasScaling && !mScaleDragDetector.isScaling
            val didntDrag = !wasDragging && !mScaleDragDetector.isDragging
            mBlockParentIntercept = didntScale && didntDrag
            // Check to see if the user double tapped
        }
        return handled
    }

    private fun setScale(scale: Float, animate: Boolean) {
        setScale(
            scale, (mImageView.right / 2).toFloat(), (mImageView.bottom / 2).toFloat(), animate
        )
    }

    fun setScale(
        scale: Float, focalX: Float, focalY: Float, animate: Boolean
    ) {
        // Check to see if the scale is within bounds
        require(!(scale < mMinScale || scale > mMaxScale)) { "Scale must be within the range of minScale and maxScale" }
        if (animate) {
            mImageView.post(
                AnimatedZoomRunnable(
                    this.scale, scale, focalX, focalY
                )
            )
        } else {
            mSuppMatrix.setScale(scale, scale, focalX, focalY)
            checkAndDisplayMatrix()
        }
    }

    /**
     * Set the zoom interpolator
     *
     * @param interpolator the zoom interpolator
     */
    fun setZoomInterpolator(interpolator: Interpolator) {
        mInterpolator = interpolator
    }

    var isZoomable: Boolean
        get() = isZoomEnabled
        set(zoomable) {
            isZoomEnabled = zoomable
            update()
        }

    fun update() {
        if (isZoomEnabled) {
            // Update the base matrix using the current drawable
            updateBaseMatrix(mImageView.drawable)
        } else {
            // Reset the Matrix...
            resetMatrix()
        }
    }

    private val drawMatrix: Matrix
        private get() {
            imageMatrix.set(mBaseMatrix)
            imageMatrix.postConcat(mSuppMatrix)
            return imageMatrix
        }

    fun setZoomTransitionDuration(milliseconds: Int) {
        mZoomDuration = milliseconds
    }

    /**
     * Helper method that 'unpacks' a Matrix and returns the required value
     *
     * @param matrix     Matrix to unpack
     * @param whichValue Which value from Matrix.M* to return
     * @return returned value
     */
    private fun getValue(matrix: Matrix, whichValue: Int): Float {
        matrix.getValues(mMatrixValues)
        return mMatrixValues[whichValue]
    }

    /**
     * Resets the Matrix back to FIT_CENTER, and then displays its contents
     */
    private fun resetMatrix() {
        mSuppMatrix.reset()
        setRotationBy(mBaseRotation)
        setImageViewMatrix(drawMatrix)
        checkMatrixBounds()
    }

    private fun setImageViewMatrix(matrix: Matrix) {
        mImageView.imageMatrix = matrix
    }

    /**
     * Helper method that simply checks the Matrix, and then displays the result
     */
    private fun checkAndDisplayMatrix() {
        if (checkMatrixBounds()) {
            setImageViewMatrix(drawMatrix)
        }
    }

    /**
     * Helper method that maps the supplied Matrix to the current Drawable
     *
     * @param matrix - Matrix to map Drawable against
     * @return RectF - Displayed Rectangle
     */
    private fun getDisplayRect(matrix: Matrix): RectF? {
        val d = mImageView.drawable
        if (d != null) {
            mDisplayRect.set(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
            matrix.mapRect(mDisplayRect)
            return mDisplayRect
        }
        return null
    }

    /**
     * Calculate Matrix for FIT_CENTER
     *
     * @param drawable - Drawable being displayed
     */
    private fun updateBaseMatrix(drawable: Drawable?) {
        if (drawable == null) {
            return
        }
        val viewWidth = getImageViewWidth(mImageView).toFloat()
        val viewHeight = getImageViewHeight(mImageView).toFloat()
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        mBaseMatrix.reset()
        val widthScale = viewWidth / drawableWidth
        val heightScale = viewHeight / drawableHeight
        if (mScaleType == ScaleType.CENTER) {
            mBaseMatrix.postTranslate(
                (viewWidth - drawableWidth) / 2f, (viewHeight - drawableHeight) / 2f
            )
        } else if (mScaleType == ScaleType.CENTER_CROP) {
            val scale = Math.max(widthScale, heightScale)
            mBaseMatrix.postScale(scale, scale)
            mBaseMatrix.postTranslate(
                (viewWidth - drawableWidth * scale) / 2f, (viewHeight - drawableHeight * scale) / 2f
            )
        } else if (mScaleType == ScaleType.CENTER_INSIDE) {
            val scale = Math.min(1.0f, Math.min(widthScale, heightScale))
            mBaseMatrix.postScale(scale, scale)
            mBaseMatrix.postTranslate(
                (viewWidth - drawableWidth * scale) / 2f, (viewHeight - drawableHeight * scale) / 2f
            )
        } else {
            var mTempSrc = RectF(0f, 0f, drawableWidth.toFloat(), drawableHeight.toFloat())
            val mTempDst = RectF(0f, 0f, viewWidth, viewHeight)
            if (mBaseRotation.toInt() % 180 != 0) {
                mTempSrc = RectF(0f, 0f, drawableHeight.toFloat(), drawableWidth.toFloat())
            }
            when (mScaleType) {
                ScaleType.FIT_CENTER -> mBaseMatrix.setRectToRect(
                    mTempSrc, mTempDst, ScaleToFit.CENTER
                )

                ScaleType.FIT_START -> mBaseMatrix.setRectToRect(
                    mTempSrc, mTempDst, ScaleToFit.START
                )

                ScaleType.FIT_END -> mBaseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.END)
                ScaleType.FIT_XY -> mBaseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.FILL)
                else -> {}
            }
        }
        resetMatrix()
    }

    private fun checkMatrixBounds(): Boolean {
        val rect = getDisplayRect(drawMatrix) ?: return false
        val height = rect.height()
        val width = rect.width()
        var deltaX = 0f
        var deltaY = 0f
        val viewHeight = getImageViewHeight(mImageView)
        if (height <= viewHeight) {
            deltaY = when (mScaleType) {
                ScaleType.FIT_START -> -rect.top
                ScaleType.FIT_END -> viewHeight - height - rect.top
                else -> (viewHeight - height) / 2 - rect.top
            }
            mVerticalScrollEdge = VERTICAL_EDGE_BOTH
        } else if (rect.top > 0) {
            mVerticalScrollEdge = VERTICAL_EDGE_TOP
            deltaY = -rect.top
        } else if (rect.bottom < viewHeight) {
            mVerticalScrollEdge = VERTICAL_EDGE_BOTTOM
            deltaY = viewHeight - rect.bottom
        } else {
            mVerticalScrollEdge = VERTICAL_EDGE_NONE
        }
        val viewWidth = getImageViewWidth(mImageView)
        if (width <= viewWidth) {
            deltaX = when (mScaleType) {
                ScaleType.FIT_START -> -rect.left
                ScaleType.FIT_END -> viewWidth - width - rect.left
                else -> (viewWidth - width) / 2 - rect.left
            }
            mHorizontalScrollEdge = HORIZONTAL_EDGE_BOTH
        } else if (rect.left > 0) {
            mHorizontalScrollEdge = HORIZONTAL_EDGE_LEFT
            deltaX = -rect.left
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right
            mHorizontalScrollEdge = HORIZONTAL_EDGE_RIGHT
        } else {
            mHorizontalScrollEdge = HORIZONTAL_EDGE_NONE
        }
        // Finally actually translate the matrix
        mSuppMatrix.postTranslate(deltaX, deltaY)
        return true
    }

    private fun getImageViewWidth(imageView: ImageView): Int {
        return imageView.width - imageView.paddingLeft - imageView.paddingRight
    }

    private fun getImageViewHeight(imageView: ImageView): Int {
        return imageView.height - imageView.paddingTop - imageView.paddingBottom
    }

    private fun cancelFling() {
        if (mCurrentFlingRunnable != null) {
            mCurrentFlingRunnable!!.cancelFling()
            mCurrentFlingRunnable = null
        }
    }

    private inner class AnimatedZoomRunnable(
        currentZoom: Float,
        targetZoom: Float,
        private val mFocalX: Float,
        private val mFocalY: Float
    ) : Runnable {
        private val mStartTime: Long = System.currentTimeMillis()
        private val mZoomStart: Float
        private val mZoomEnd: Float

        init {
            mZoomStart = currentZoom
            mZoomEnd = targetZoom
        }

        override fun run() {
            val t = interpolate()
            val interpolatedScale = mZoomStart + t * (mZoomEnd - mZoomStart)
            val deltaScale: Float = interpolatedScale / scale
            onGestureListener.onScale(deltaScale, mFocalX, mFocalY)
            // We haven't hit our target scale yet, so post ourselves again
            if (t < 1f) {
                mImageView.postOnAnimation(this)
            }
        }

        private fun interpolate(): Float {
            var t = 1f * (System.currentTimeMillis() - mStartTime) / mZoomDuration
            t = t.coerceAtMost(1f)
            t = mInterpolator.getInterpolation(t)
            return t
        }
    }

    private inner class FlingRunnable(context: Context?) : Runnable {
        private val mScroller: OverScroller
        private var mCurrentX = 0
        private var mCurrentY = 0

        init {
            mScroller = OverScroller(context)
        }

        fun cancelFling() {
            mScroller.forceFinished(true)
        }

        fun fling(
            viewWidth: Int, viewHeight: Int, velocityX: Int, velocityY: Int
        ) {
            val rect: RectF = displayRect ?: return
            val startX = (-rect.left).roundToInt()
            val minX: Int
            val maxX: Int
            val minY: Int
            val maxY: Int
            if (viewWidth < rect.width()) {
                minX = 0
                maxX = (rect.width() - viewWidth).roundToInt()
            } else {
                maxX = startX
                minX = maxX
            }
            val startY = Math.round(-rect.top)
            if (viewHeight < rect.height()) {
                minY = 0
                maxY = Math.round(rect.height() - viewHeight)
            } else {
                maxY = startY
                minY = maxY
            }
            mCurrentX = startX
            mCurrentY = startY
            // If we actually can move, fling the scroller
            if (startX != maxX || startY != maxY) {
                mScroller.fling(
                    startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0
                )
            }
        }

        override fun run() {
            if (mScroller.isFinished) {
                return  // remaining post that should not be handled
            }
            if (mScroller.computeScrollOffset()) {
                val newX = mScroller.currX
                val newY = mScroller.currY
                mSuppMatrix.postTranslate(
                    (mCurrentX - newX).toFloat(), (mCurrentY - newY).toFloat()
                )
                checkAndDisplayMatrix()
                mCurrentX = newX
                mCurrentY = newY
                mImageView.postOnAnimation(this)
            }
        }
    }

    companion object {
        private const val DEFAULT_MAX_SCALE = 3.0f
        private const val DEFAULT_MID_SCALE = 1.75f
        private const val DEFAULT_MIN_SCALE = 1.0f
        private const val DEFAULT_ZOOM_DURATION = 200
        private const val HORIZONTAL_EDGE_NONE = -1
        private const val HORIZONTAL_EDGE_LEFT = 0
        private const val HORIZONTAL_EDGE_RIGHT = 1
        private const val HORIZONTAL_EDGE_BOTH = 2
        private const val VERTICAL_EDGE_NONE = -1
        private const val VERTICAL_EDGE_TOP = 0
        private const val VERTICAL_EDGE_BOTTOM = 1
        private const val VERTICAL_EDGE_BOTH = 2
    }
}
