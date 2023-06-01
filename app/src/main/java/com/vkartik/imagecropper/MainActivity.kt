package com.vkartik.imagecropper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val img = findViewById<PhotoView>(R.id.iv)
        val inputStream = resources.openRawResource(R.raw.img)
        var bitmap = BitmapFactory.decodeStream(inputStream)
        val (screenWidth, screenHeight) = getScreenParams(windowManager)

        val maxWidth = screenWidth.toFloat() * 0.9f
        val maxHeight = screenHeight.toFloat() * 0.8f
        if (bitmap.width > bitmap.height) {
            val aspectRatio = bitmap.height.toDouble() / bitmap.width.toDouble()
            val height = (aspectRatio * maxWidth).toInt()
            bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth.roundToInt(), height, false)
        } else {
            val aspectRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
            val width = (aspectRatio * maxHeight).toInt()
            bitmap = Bitmap.createScaledBitmap(bitmap, width, maxHeight.roundToInt(), false)
        }

        img.setImageBitmap(bitmap)
//        img.enableZoom(false)
    }
}