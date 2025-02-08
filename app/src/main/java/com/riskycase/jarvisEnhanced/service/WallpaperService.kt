package com.riskycase.jarvisEnhanced.service

import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.SweepGradient
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.riskycase.jarvisEnhanced.util.ClockConstants
import com.riskycase.jarvisEnhanced.util.MediaUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@AndroidEntryPoint
class WallpaperService : WallpaperService() {

    private var backgroundImage: Bitmap? = null
        set(it) {
            it?.also { bitmap ->
                val backgroundImagePalette = Palette.Builder(bitmap).generate()
                baseColor = backgroundImagePalette.getVibrantColor(Color.WHITE)
                val darkColorHSL = floatArrayOf(0f, 0f, 0f)
                ColorUtils.colorToHSL(
                    backgroundImagePalette.getDarkVibrantColor(Color.DKGRAY), darkColorHSL
                )
                darkerBaseColor =
                    ColorUtils.HSLToColor(floatArrayOf(darkColorHSL[0], darkColorHSL[1], 0.47f))
                darkBaseColor =
                    ColorUtils.HSLToColor(floatArrayOf(darkColorHSL[0], darkColorHSL[1], 0.77f))
                colorOnBaseColor =
                    if (ColorUtils.calculateLuminance(baseColor) > 0.5f) Color.BLACK else Color.WHITE
            }
            field = it
        }
    private var baseColor = Color.WHITE
    private var darkBaseColor = Color.GRAY
    private var darkerBaseColor = Color.DKGRAY
    private var colorOnBaseColor = Color.BLACK

    @Inject
    lateinit var batteryManager: BatteryManager

    @Inject
    lateinit var keyguardManager: KeyguardManager

    @Inject
    lateinit var clockConstants: ClockConstants

    @Inject
    lateinit var mediaUtils: MediaUtils

    @Inject
    @Named("OddDateFormat")
    lateinit var oddSimpleDateFormat: SimpleDateFormat

    @Inject
    @Named("EvenDateFormat")
    lateinit var evenSimpleDateFormat: SimpleDateFormat

    @Inject
    @Named("7SegmentAllOnString")
    lateinit var allOnString: String

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
            clockConstants.updateVisibility(visible)
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
            clockConstants.updateDimensions(width, height)
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
                val clockCenter = clockConstants.getClockCenter()
                if (((clockCenter.x - x.toFloat()).pow(2) + (clockCenter.y - y.toFloat()).pow(2)) < (clockConstants.getClockRadius()
                        .pow(2))
                ) {
                    val openAlarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                    openAlarmIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    applicationContext.startActivity(openAlarmIntent)
                }
                mediaUtils.handleTouchInput(x, y)
            }
            return Bundle()
        }

        private fun drawHand(
            canvas: Canvas,
            center: PointF,
            width: Float,
            height: Float,
            constant: Int,
            handPaint: Paint,
            radius: Float
        ) {
            val rotation = clockConstants.getRotation(constant)
            val shadowPaint = clockConstants.getClockShadowPaint()
            val sinValue = sin(rotation * PI / 180f).toFloat()
            val cosValue = cos(rotation * PI / 180f).toFloat()

            val start = PointF(
                width.times(0.5f) * -cosValue, width.times(0.5f) * -sinValue
            ) // width/2, rotation - 180
            val heightOutwards = PointF(
                height * sinValue, height * -cosValue
            ) // height, rotation - 90
            val widthCW = PointF(
                width * cosValue, width * sinValue
            ) // width, rotation


            val handPath = Path()
            handPath.moveTo(center.x, center.y)
            handPath.rMoveTo(start.x, start.y)
            handPath.rLineTo(heightOutwards.x, heightOutwards.y)
            handPath.rLineTo(widthCW.x, widthCW.y)
            handPath.rLineTo(-heightOutwards.x, -heightOutwards.y)
            handPath.close()
            canvas.drawPath(handPath, handPaint)

            val shadowPath = Path()
            shadowPath.moveTo(center.x, center.y)
            shadowPath.rMoveTo(start.x, start.y)
            if (rotation < 45f || rotation > 315f) {
                shadowPath.rLineTo(widthCW.x, widthCW.y)
                shadowPath.rLineTo(heightOutwards.x, heightOutwards.y)
                shadowPath.rLineTo(radius.times(2f), radius.times(2f))
                shadowPath.rLineTo(-heightOutwards.x, -heightOutwards.y)
                shadowPath.rLineTo(-widthCW.x, -widthCW.y)
            } else if (rotation < 135f) {
                shadowPath.rMoveTo(widthCW.x, widthCW.y)
                shadowPath.rLineTo(heightOutwards.x, heightOutwards.y)
                shadowPath.rLineTo(-widthCW.x, -widthCW.y)
                shadowPath.rLineTo(radius.times(2f), radius.times(2f))
                shadowPath.rLineTo(widthCW.x, widthCW.y)
                shadowPath.rLineTo(-heightOutwards.x, -heightOutwards.y)
            } else if (rotation < 215f) {
                shadowPath.rMoveTo(widthCW.x, widthCW.y)
                shadowPath.rMoveTo(heightOutwards.x, heightOutwards.y)
                shadowPath.rLineTo(-widthCW.x, -widthCW.y)
                shadowPath.rLineTo(-heightOutwards.x, -heightOutwards.y)
                shadowPath.rLineTo(radius.times(2f), radius.times(2f))
                shadowPath.rLineTo(heightOutwards.x, heightOutwards.y)
                shadowPath.rLineTo(widthCW.x, widthCW.y)
            } else {
                shadowPath.rMoveTo(heightOutwards.x, heightOutwards.y)
                shadowPath.rLineTo(-heightOutwards.x, -heightOutwards.y)
                shadowPath.rLineTo(widthCW.x, widthCW.y)
                shadowPath.rLineTo(radius.times(2f), radius.times(2f))
                shadowPath.rLineTo(-widthCW.x, -widthCW.y)
                shadowPath.rLineTo(heightOutwards.x, heightOutwards.y)
            }
            shadowPath.close()

            val centerPath = Path()
            centerPath.moveTo(center.x, center.y)
            centerPath.rMoveTo((25f.plus(7.5f / 2)) / sqrt(2f), -(25f.plus(7.5f / 2)) / sqrt(2f))
            centerPath.rLineTo(radius * 2f, radius * 2f)
            centerPath.rLineTo(-(25f.plus(7.5f / 2)) * sqrt(2f), (25f.plus(7.5f / 2)) * sqrt(2f))
            centerPath.rLineTo(-radius * 2f, -radius * 2f)
            centerPath.close()
            shadowPath.op(centerPath, Path.Op.UNION)

            val clockFacePath = Path()
            clockFacePath.addCircle(center.x, center.y, radius, Path.Direction.CW)
            shadowPath.op(clockFacePath, Path.Op.INTERSECT)

            canvas.drawPath(shadowPath, shadowPaint)

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

        private fun drawClock(canvas: Canvas) {

            val center = clockConstants.getClockCenter()
            val radius = clockConstants.getClockRadius()

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
            bodyPaint.color = baseColor
            bodyPaint.style = Paint.Style.FILL

            // Main face
            canvas.drawCircle(center.x, center.y, radius.minus(7.5f), bodyPaint)

            bodyPaint.style = Paint.Style.STROKE
            bodyPaint.strokeWidth = 10f
            // Border outline
            canvas.drawCircle(center.x, center.y, radius.plus(7.5f), bodyPaint)

            val now = Calendar.getInstance()
            val contrastColor = colorOnBaseColor

            if (!keyguardManager.isKeyguardLocked) {
                val clockTextPaint = clockConstants.getClockTextPaint()
                val currentTimeText =
                    (if (now.get(Calendar.SECOND) % 2 == 0) evenSimpleDateFormat else oddSimpleDateFormat).format(
                        now
                    )
                val textBounds = clockConstants.getTextBounds()
                clockTextPaint.color = darkerBaseColor
                canvas.drawRect(
                    RectF(
                        center.x - (textBounds.width().toFloat() / 2f) - 20f,
                        center.y - (radius / 2f) - (textBounds.height().toFloat()) + 10f,
                        center.x + (textBounds.width().toFloat() / 2f) + 17.5f,
                        center.y - (radius / 2f) + 47.5f
                    ), clockTextPaint
                )
                clockTextPaint.color = darkBaseColor
                canvas.drawRect(
                    RectF(
                        center.x - (textBounds.width().toFloat() / 2f) - 10f,
                        center.y - (radius / 2f) - (textBounds.height().toFloat()) + 20f,
                        center.x + (textBounds.width().toFloat() / 2f) + 10f,
                        center.y - (radius / 2f) + 40f
                    ), clockTextPaint
                )
                clockTextPaint.color = Color.argb(96, 128, 128, 128)
                canvas.drawText(
                    allOnString,
                    center.x - (textBounds.width().toFloat() / 2f),
                    center.y - (radius / 2f) + 30f,
                    clockTextPaint
                )
                clockTextPaint.color = contrastColor
                canvas.drawText(
                    currentTimeText,
                    center.x - (textBounds.width().toFloat() / 2f),
                    center.y - (radius / 2f) + 30f,
                    clockTextPaint
                )
            }

            val clockPaint = Paint()
            clockPaint.color = contrastColor
            clockPaint.style = Paint.Style.FILL

            // Hours hand
            drawHand(
                canvas,
                center,
                15f,
                radius.times(0.65f),
                Calendar.HOUR,
                clockPaint,
                radius.minus(7.5f)
            )

            // Minutes hand
            drawHand(
                canvas,
                center,
                10f,
                radius.minus(7.5f),
                Calendar.MINUTE,
                clockPaint,
                radius.minus(7.5f)
            )

            // Seconds hand
            drawHand(
                canvas,
                center,
                4f,
                radius.minus(7.5f),
                Calendar.SECOND,
                clockPaint,
                radius.minus(7.5f)
            )

            // Center piece
            bodyPaint.style = Paint.Style.FILL
            canvas.drawCircle(center.x, center.y, 25f, bodyPaint)
            clockPaint.style = Paint.Style.STROKE
            clockPaint.strokeWidth = 7.5f
            canvas.drawCircle(center.x, center.y, 25f, clockPaint)
        }

        private fun draw() {
            val nextDraw = SystemClock.uptimeMillis() + (1000 / 24)
            val holder = surfaceHolder
            val canvas: Canvas?
            clockConstants.updateVisibility(visible)
            if (visible) {
                canvas = holder.lockHardwareCanvas()
                if (canvas != null) {
                    if (backgroundImage != null) {
                        canvas.save()
                        canvas.translate(if (!isPreview) width * xOffset else 0f, 0f)
                        drawImageCover(canvas, backgroundImage!!, width, height)
                        canvas.restore()
                    } else {
                        val blackPaint = Paint()
                        blackPaint.color = Color.BLACK
                        blackPaint.style = Paint.Style.FILL
                        canvas.drawRect(
                            RectF(0f, 0f, width.toFloat(), height.toFloat()), blackPaint
                        )
                    }
                    if (!keyguardManager.isKeyguardLocked) {
                        mediaUtils.drawMediaPlayer(canvas, baseColor, colorOnBaseColor)
                    }
                    drawClock(canvas)
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postAtTime(drawRunner, nextDraw)
        }

    }
}