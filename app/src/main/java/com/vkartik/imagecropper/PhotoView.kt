package com.vkartik.imagecropper

import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView


/**
 * A zoomable ImageView. See [PhotoViewAttacher] for most of the details on how the zooming
 * is accomplished
 */
class PhotoView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) :
    AppCompatImageView(context, attr, defStyle) {
    /**
     * Get the current [PhotoViewAttacher] for this view. Be wary of holding on to references
     * to this attacher, as it has a reference to this view, which, if a reference is held in the
     * wrong place, can cause memory leaks.
     *
     * @return the attacher.
     */
    private var attacher: PhotoViewAttacher = PhotoViewAttacher(this)
    private var pendingScaleType: ScaleType? = null

    init {
        super.setScaleType(ScaleType.MATRIX)
        //apply the previously applied scale type
        if (pendingScaleType != null) {
            scaleType = pendingScaleType!!
            pendingScaleType = null
        }
    }

    override fun getScaleType(): ScaleType {
        return attacher.scaleType
    }

    override fun getImageMatrix(): Matrix {
        return attacher.imageMatrix
    }

    override fun setScaleType(scaleType: ScaleType) {
        attacher.scaleType = scaleType
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        // setImageBitmap calls through to this method
        attacher.update()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        attacher.update()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        attacher.update()
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val changed = super.setFrame(l, t, r, b)
        if (changed) {
            attacher.update()
        }
        return changed
    }

    fun enableZoom(zoomable: Boolean) {
        attacher.isZoomable = zoomable
    }
}
