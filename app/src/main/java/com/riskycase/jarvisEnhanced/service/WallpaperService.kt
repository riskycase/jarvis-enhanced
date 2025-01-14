package com.riskycase.jarvisEnhanced.service

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SweepGradient
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.riskycase.jarvisEnhanced.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class WallpaperService : WallpaperService() {

    private var backgroundImage: Bitmap? = null

    @Inject
    lateinit var batteryManager: BatteryManager

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
        private var clockCenter: PointF = PointF(0f, 0f)
        private var radius: Float = 0f

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
            holder: SurfaceHolder?, format: Int, width: Int, height: Int
        ) {
            this.width = width
            this.height = height
            clockCenter = PointF(width / 2f, width * 3f / 4f)
            radius = width / 3f
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

        private fun drawClock(
            canvas: Canvas,
            center: PointF,
            radius: Float,
            palette: Palette,
        ) {
            val batteryPaint = Paint()
            batteryPaint.shader = SweepGradient(center.x, center.y, Color.RED, Color.GREEN).apply {
                val rotationMatrix = Matrix()
                rotationMatrix.preRotate(-90f, center.x, center.y)
                setLocalMatrix(rotationMatrix)
            }
            batteryPaint.style = Paint.Style.STROKE
            batteryPaint.strokeWidth = 20f

            canvas.drawArc(
                RectF(
                    center.x - radius, center.y - radius, center.x + radius, center.y + radius
                ),
                -90f,
                (batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) * 3.6f),
                false,
                batteryPaint
            )

            val bodyPaint = Paint()
            bodyPaint.color = palette.getVibrantColor(Color.WHITE)
            bodyPaint.style = Paint.Style.FILL

            // Main face
            canvas.drawCircle(center.x, center.y, radius.minus(7.5f), bodyPaint)

            bodyPaint.style = Paint.Style.STROKE
            bodyPaint.strokeWidth = 10f
            // Border outline
            canvas.drawCircle(center.x, center.y, radius.plus(7.5f), bodyPaint)

            val now = Calendar.getInstance()
            val contrastColor =
                if (ColorUtils.calculateLuminance(palette.getVibrantColor(Color.WHITE)) > 0.5f) Color.BLACK else Color.WHITE

            val textPaint = Paint()
            textPaint.typeface = resources.getFont(R.font.dseg7modernmini)
            textPaint.textSize = radius / 6f
            var textBounds = Rect()
            val currentTimeText = SimpleDateFormat(if(now.get(Calendar.SECOND) % 2 == 0) "HH:mm:ss" else "HH mm ss", Locale.UK).format(now)
            textPaint.getTextBounds("88:88:88", 0, 8, textBounds)
            var darkColorHSL = floatArrayOf(0f, 0f, 0f)
            ColorUtils.colorToHSL(palette.getDarkVibrantColor(Color.DKGRAY), darkColorHSL)
            textPaint.color =
                ColorUtils.HSLToColor(floatArrayOf(darkColorHSL[0], darkColorHSL[1], 0.47f))
            canvas.drawRect(
                RectF(
                    center.x - (textBounds.width().toFloat() / 2f) - 20f,
                    center.y - (radius / 2f) - (textBounds.height().toFloat()) + 10f,
                    center.x + (textBounds.width().toFloat() / 2f) + 17.5f,
                    center.y - (radius / 2f) + 47.5f
                ), textPaint
            )
            textPaint.color =
                ColorUtils.HSLToColor(floatArrayOf(darkColorHSL[0], darkColorHSL[1], 0.77f))
            canvas.drawRect(
                RectF(
                    center.x - (textBounds.width().toFloat() / 2f) - 10f,
                    center.y - (radius / 2f) - (textBounds.height().toFloat()) + 20f,
                    center.x + (textBounds.width().toFloat() / 2f) + 10f,
                    center.y - (radius / 2f) + 40f
                ), textPaint
            )
            textPaint.color = Color.argb(128, 128, 128, 128)
            canvas.drawText(
                "88:88:88",
                center.x - (textBounds.width().toFloat() / 2f),
                center.y - (radius / 2f) + 30f,
                textPaint
            )
            textPaint.color = contrastColor
            canvas.drawText(
                currentTimeText,
                center.x - (textBounds.width().toFloat() / 2f),
                center.y - (radius / 2f) + 30f,
                textPaint
            )

            val clockPaint = Paint()
            clockPaint.color = contrastColor
            clockPaint.style = Paint.Style.FILL

            // Hours hand
            canvas.save()
            canvas.rotate(
                ((now.get(Calendar.HOUR).toFloat() / 12f) + (now.get(Calendar.MINUTE)
                    .toFloat() / 720f) + (now.get(Calendar.SECOND).toFloat() / 43200f) + now.get(
                    Calendar.MILLISECOND
                ).toFloat() / 43200000f) * 360f, center.x, center.y
            )
            canvas.drawRect(
                RectF(
                    center.x - 7.5f, center.y - (radius * 0.65f), center.x + 7.5f, center.y
                ), clockPaint
            )
            canvas.restore()

            // Minutes hand
            canvas.save()
            canvas.rotate(
                ((now.get(Calendar.MINUTE).toFloat() / 60f) + (now.get(Calendar.SECOND)
                    .toFloat() / 3600f) + now.get(
                    Calendar.MILLISECOND
                ).toFloat() / 3600000f) * 360f, center.x, center.y
            )
            canvas.drawRect(
                RectF(
                    center.x - 5f, center.y - (radius * 0.95f), center.x + 5f, center.y
                ), clockPaint
            )
            canvas.restore()

            // Seconds hand
            canvas.save()
            canvas.rotate(
                ((now.get(Calendar.SECOND).toFloat() / 60f) + now.get(
                    Calendar.MILLISECOND
                ).toFloat() / 60000f) * 360f, center.x, center.y
            )
            canvas.drawRect(
                RectF(
                    center.x - 2f, center.y - (radius * 0.95f), center.x + 2f, center.y
                ), clockPaint
            )
            canvas.restore()

            // Center piece
            bodyPaint.style = Paint.Style.FILL
            canvas.drawCircle(center.x, center.y, 30f, bodyPaint)
            clockPaint.style = Paint.Style.STROKE
            clockPaint.strokeWidth = 10f
            canvas.drawCircle(center.x, center.y, 30f, clockPaint)
        }

        private fun draw() {
            val nextDraw = SystemClock.uptimeMillis() + (1000 / 24)
            val holder = surfaceHolder
            val canvas: Canvas?
            if (visible) {
                canvas = holder.lockHardwareCanvas()
                if (canvas != null) {
                    if (backgroundImage != null) {
                        canvas.save()
                        canvas.translate(if (!isPreview) width * xOffset else 0f, 0f)
                        drawImageCover(canvas, backgroundImage!!, width, height)
                        canvas.restore()
                        drawClock(
                            canvas, clockCenter, radius, Palette.from(backgroundImage!!).generate()
                        )
                    } else {
                        val blackPaint = Paint()
                        blackPaint.color = Color.BLACK
                        blackPaint.style = Paint.Style.FILL
                        canvas.drawRect(
                            RectF(0f, 0f, width.toFloat(), height.toFloat()), blackPaint
                        )
                        drawClock(
                            canvas,
                            clockCenter,
                            radius,
                            Palette.from(listOf(Palette.Swatch(Color.WHITE, 100)))
                        )
                    }
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postAtTime(drawRunner, nextDraw)
        }

    }
}