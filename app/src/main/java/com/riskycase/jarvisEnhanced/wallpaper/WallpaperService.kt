package com.riskycase.jarvisEnhanced.wallpaper

import android.app.KeyguardManager
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.graphics.withTranslation
import com.riskycase.jarvisEnhanced.util.SystemServicesContainer
import com.riskycase.jarvisEnhanced.util.wallpaper.BackgroundImageUtils
import com.riskycase.jarvisEnhanced.util.wallpaper.ClockUtils
import com.riskycase.jarvisEnhanced.util.wallpaper.MediaUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.max

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

    @Inject
    lateinit var systemServicesContainer: SystemServicesContainer

    override fun onCreateEngine(): Engine {
        return WallpaperEngine()
    }

    inner class WallpaperEngine : Engine() {

        private val handler = Handler(Looper.myLooper()!!)
        private val drawRunner = Runnable { draw() }

        private var readyToDraw: Boolean = false

        private var holder: SurfaceHolder? = null

        private var width: Int = 0
        private var height: Int = 0
        private var visible: Boolean = true
        private var xOffset: Float = 0f

        init {
            handler.post(drawRunner)
            systemServicesContainer.wallpaperEngine = this
        }

        override fun onComputeColors(): WallpaperColors? {
            return backgroundImageUtils.getBackgroundImage()?.let { WallpaperColors.fromBitmap(it) }
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

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            this.readyToDraw = true
            this.holder = holder
            onVisibilityChanged(visible)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            this.visible = false
            this.readyToDraw = false
            handler.removeCallbacks(drawRunner)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?, format: Int, width: Int, height: Int
        ) {
            this.holder = holder
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
                width.toFloat() / bitmap.width,
                height.toFloat() / bitmap.height
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
            val canvas: Canvas?
            clockUtils.updateVisibility(visible)
            holder?.let { holder ->
                if (visible && readyToDraw && holder.surface.isValid) {
                    canvas = holder.lockHardwareCanvas() ?: holder.lockCanvas()
                    if (canvas != null) {
                        backgroundImageUtils.getBackgroundImage()?.also { backgroundImage ->
                            canvas.withTranslation(if (!isPreview) width * xOffset * 0.5f else 0f, 0f) {
                                try {
                                    drawImageCover(this, backgroundImage, width, height)
                                } catch (exception: RuntimeException) {
                                    backgroundImageUtils.getSmallBackgroundImage()
                                        ?.also { smallBackgroundImage ->
                                            drawImageCover(this, smallBackgroundImage, width, height)
                                        }
                                }
                            }
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
            }
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postAtTime(drawRunner, nextDraw)
        }

    }
}