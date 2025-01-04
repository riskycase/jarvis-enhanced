package com.riskycase.jarvisEnhanced.service

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.max

class WallpaperService : WallpaperService() {

    private var backgroundImage: Bitmap? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        applicationContext.openFileInput("wallpaper").use {
            backgroundImage = BitmapFactory.decodeStream(it)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreateEngine(): Engine {
        return MyWallpaperEngine()
    }

    inner class MyWallpaperEngine : Engine() {

        private val handler = Handler(Looper.myLooper()!!)
        private val drawRunner = { draw() }

        private var width: Int = 0
        private var height: Int = 0
        private var visible: Boolean = true
        private var xOffset: Float = 0f

        init {
            handler.post(drawRunner)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                handler.post(drawRunner)
            } else {
                handler.removeCallbacks(drawRunner)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            this.visible = false
            handler.removeCallbacks(drawRunner)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            this.width = width
            this.height = height
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(
                xOffset,
                yOffset,
                xOffsetStep,
                yOffsetStep,
                xPixelOffset,
                yPixelOffset
            )
            this.xOffset = xOffset - 0.5f
        }

        private fun drawImageCover(
            canvas: Canvas,
            bitmap: Bitmap,
            canvasWidth: Int,
            canvasHeight: Int
        ) {
            val scaleFactor = max(
                desiredMinimumWidth.toFloat() / bitmap.width,
                desiredMinimumHeight.toFloat() / bitmap.height
            )

            val scaledWidth = (bitmap.width * scaleFactor).toInt()
            val scaledHeight = (bitmap.height * scaleFactor).toInt()

            val x = (canvasWidth - scaledWidth) / 2f
            val y = (canvasHeight - scaledHeight) / 2f

            val rect = RectF(x, y, x + scaledWidth, y + scaledHeight)
            canvas.drawBitmap(bitmap, null, rect, null)
        }

        private fun draw() {
            val nextDraw = SystemClock.uptimeMillis() + (1000 / 60)
            val holder = surfaceHolder
            val canvas: Canvas?
            if (visible) {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.save()
                    canvas.translate(if (!isPreview) width * xOffset else 0f, 0f)
                    backgroundImage?.let { drawImageCover(canvas, it, width, height) }
                    canvas.restore()
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postAtTime(drawRunner, nextDraw)
        }

    }
}