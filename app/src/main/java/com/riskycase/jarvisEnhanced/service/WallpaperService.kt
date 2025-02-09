package com.riskycase.jarvisEnhanced.service

import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.riskycase.jarvisEnhanced.util.wallpaper.BackgroundImageUtils
import com.riskycase.jarvisEnhanced.util.wallpaper.ClockUtils
import com.riskycase.jarvisEnhanced.util.wallpaper.MediaUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.pow

@AndroidEntryPoint
class WallpaperService : WallpaperService() {

    @Inject
    lateinit var keyguardManager: KeyguardManager

    @Inject
    lateinit var clockUtils: ClockUtils

    @Inject
    lateinit var mediaUtils: MediaUtils

    @Inject
    lateinit var backgroundImageUtils: BackgroundImageUtils

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
            clockUtils.updateVisibility(visible)
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
            holder: SurfaceHolder?, format: Int, width: Int, height: Int
        ) {
            this.width = width
            this.height = height
            clockUtils.updateDimensions(width, height)
            mediaUtils.updateDimensions(width, height)
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
                xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset
            )
            this.xOffset = xOffset - 0.5f
        }

        override fun onCommand(
            action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean
        ): Bundle {
            if (WallpaperManager.COMMAND_TAP == action && !keyguardManager.isKeyguardLocked) {
                clockUtils.handleTouchEvent(x, y)
                mediaUtils.handleTouchInput(x, y)
            }
            return Bundle()
        }

        private fun drawImageCover(
            canvas: Canvas, bitmap: Bitmap, canvasWidth: Int, canvasHeight: Int
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
            val nextDraw = SystemClock.uptimeMillis() + (1000 / 30)
            val holder = surfaceHolder
            val canvas: Canvas?
            clockUtils.updateVisibility(visible)
            if (visible) {
                canvas = holder.lockHardwareCanvas()
                if (canvas != null) {
                    backgroundImageUtils.getBackgroundImage()?.also { backgroundImage ->
                        canvas.save()
                        canvas.translate(if (!isPreview) width * xOffset else 0f, 0f)
                        drawImageCover(canvas, backgroundImage, width, height)
                        canvas.restore()
                    }
                    if (backgroundImageUtils.getBackgroundImage() == null) {
                        val blackPaint = Paint()
                        blackPaint.color = Color.BLACK
                        blackPaint.style = Paint.Style.FILL
                        canvas.drawRect(
                            RectF(0f, 0f, width.toFloat(), height.toFloat()), blackPaint
                        )
                    }
                    mediaUtils.drawMediaPlayer(canvas)
                    clockUtils.drawClock(canvas)
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postAtTime(drawRunner, nextDraw)
        }

    }
}