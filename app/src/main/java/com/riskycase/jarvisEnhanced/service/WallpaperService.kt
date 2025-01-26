package com.riskycase.jarvisEnhanced.service

import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SweepGradient
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.palette.graphics.Palette
import com.riskycase.jarvisEnhanced.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@AndroidEntryPoint
class WallpaperService : WallpaperService() {

    private var backgroundImage: Bitmap? = null
    private var albumArtBitmap: RoundedBitmapDrawable? = null
    private var previousMediaMetadata: MediaMetadata? = null

    @Inject
    lateinit var batteryManager: BatteryManager

    @Inject
    lateinit var keyguardManager: KeyguardManager

    @Inject
    lateinit var mediaSessionManager: MediaSessionManager

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
        private var mediaCenter: PointF = PointF(0f, 0f)
        private var controlsCenter: PointF = PointF(0f, 0f)
        private var radius: Float = 0f
        private var activeController: MediaController? = null

        private var playDrawable = getDrawable(R.drawable.play)
        private var pauseDrawable = getDrawable(R.drawable.pause)
        private var previousDrawable = getDrawable(R.drawable.previous)
        private var nextDrawable = getDrawable(R.drawable.next)

        init {
            handler.post(drawRunner)
            mediaSessionManager.addOnActiveSessionsChangedListener({ controllers ->
                activeController = if (controllers!!.size > 0) {
                    controllers[0]
                } else {
                    null
                }
            }, ComponentName(applicationContext, NotificationListener::class.java))
            if (mediaSessionManager.getActiveSessions(
                    ComponentName(
                        applicationContext,
                        NotificationListener::class.java
                    )
                ).size > 0
            ) {
                activeController = mediaSessionManager.getActiveSessions(
                    ComponentName(
                        applicationContext,
                        NotificationListener::class.java
                    )
                )[0]
            }
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
            if (WallpaperManager.COMMAND_TAP.equals(action) && !keyguardManager.isKeyguardLocked) {
                if (((clockCenter.x - x.toFloat()).pow(2) + (clockCenter.y - y.toFloat()).pow(2)) < (radius.pow(
                        2
                    ))
                ) {
                    val openAlarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                    openAlarmIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    applicationContext.startActivity(openAlarmIntent)
                }
                if (activeController != null) {
                    val mediaControlsRect = RectF(
                        controlsCenter.x - radius.div(2f).times(1.25f),
                        controlsCenter.y - radius.div(4f).times(0.85f),
                        controlsCenter.x + radius.div(2f).times(1.25f),
                        controlsCenter.y + radius.div(4f).times(0.85f),
                    )
                    if (((mediaCenter.x - x.toFloat()).pow(2) + (mediaCenter.y - y.toFloat()).pow(2)) < (radius.div(
                            2f
                        ).pow(
                            2
                        ))
                    ) {
                        activeController!!.sessionActivity?.send()
                    } else if (((controlsCenter.x - x.toFloat()).pow(2) + (controlsCenter.y - y.toFloat()).pow(
                            2
                        )) < (radius.div(4f).pow(2))
                    ) {
                        if (activeController!!.playbackState!!.isActive) {
                            activeController!!.transportControls.pause()
                        } else {
                            activeController!!.transportControls.play()
                        }
                    } else if (mediaControlsRect.contains(x.toFloat(), y.toFloat())) {
                        if (x < width.div(2f)) {
                            activeController!!.transportControls.skipToPrevious()
                        } else {
                            activeController!!.transportControls.skipToNext()
                        }
                    }
                }
            }
            return Bundle()
        }

        private fun setAlbumArt() {
            val currentMediaMetadata = activeController?.metadata
            if (currentMediaMetadata != previousMediaMetadata) {
                albumArtBitmap =
                    RoundedBitmapDrawableFactory.create(
                        resources, activeController!!.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ).apply {
                        setBounds(
                            (mediaCenter.x - radius.div(2f)).toInt(),
                            (mediaCenter.y - radius.div(2f)).toInt(),
                            (mediaCenter.x + radius.div(2f)).toInt(),
                            (mediaCenter.y + radius.div(2f)).toInt(),
                        )
                        cornerRadius = radius.div(2f)
                    }
                previousMediaMetadata = currentMediaMetadata
            }
        }

        private fun drawHand(
            canvas: Canvas,
            center: PointF,
            width: Float,
            height: Float,
            rotation: Float,
            handPaint: Paint,
            radius: Float,
            shadowPaint: Paint
        ) {
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

            if (!keyguardManager.isKeyguardLocked) {
                val textPaint = Paint()
                textPaint.typeface = resources.getFont(R.font.dseg7modernmini)
                textPaint.textSize = radius / 6f
                var textBounds = Rect()
                val currentTimeText = SimpleDateFormat(
                    if (now.get(Calendar.SECOND) % 2 == 0) "HH:mm:ss" else "HH mm ss", Locale.UK
                ).format(now)
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
                textPaint.color = Color.argb(96, 128, 128, 128)
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
            }

            val clockPaint = Paint()
            clockPaint.color = contrastColor
            clockPaint.style = Paint.Style.FILL

            val shadowPaint = Paint()
            shadowPaint.color = Color.argb(64, 0, 0, 0)
            shadowPaint.style = Paint.Style.FILL

            // Hours hand
            drawHand(
                canvas,
                center,
                15f,
                radius.times(0.65f),
                ((now.get(Calendar.HOUR).toFloat() / 12f) + (now.get(Calendar.MINUTE)
                    .toFloat() / 720f) + (now.get(Calendar.SECOND).toFloat() / 43200f) + now.get(
                    Calendar.MILLISECOND
                ).toFloat() / 43200000f) * 360f,
                clockPaint,
                radius.minus(7.5f),
                shadowPaint
            )

            // Minutes hand
            drawHand(
                canvas,
                center,
                10f,
                radius.minus(7.5f),
                ((now.get(Calendar.MINUTE).toFloat() / 60f) + (now.get(Calendar.SECOND)
                    .toFloat() / 3600f) + now.get(
                    Calendar.MILLISECOND
                ).toFloat() / 3600000f) * 360f,
                clockPaint,
                radius.minus(7.5f),
                shadowPaint
            )

            // Seconds hand
            drawHand(
                canvas,
                center,
                4f,
                radius.minus(7.5f),
                ((now.get(Calendar.SECOND).toFloat() / 60f) + now.get(
                    Calendar.MILLISECOND
                ).toFloat() / 60000f) * 360f,
                clockPaint,
                radius.minus(7.5f),
                shadowPaint
            )

            // Center piece
            bodyPaint.style = Paint.Style.FILL
            canvas.drawCircle(center.x, center.y, 25f, bodyPaint)
            clockPaint.style = Paint.Style.STROKE
            clockPaint.strokeWidth = 7.5f
            canvas.drawCircle(center.x, center.y, 25f, clockPaint)
        }

        fun drawMediaPlayer(
            canvas: Canvas,
            center: PointF,
            radius: Float,
            palette: Palette,
        ) {
            if (activeController != null) {
                canvas.save()
                canvas.rotate(
                    (activeController!!.playbackState!!.position.toFloat()).div(
                        activeController!!.metadata!!.getLong(
                            MediaMetadata.METADATA_KEY_DURATION
                        )
                    ).times(360f), center.x, center.y
                )
                albumArtBitmap?.draw(canvas)
                canvas.restore()

                val controlsPaint = Paint()
                controlsPaint.color = palette.getVibrantColor(Color.WHITE)
                controlsPaint.style = Paint.Style.FILL
                controlsPaint.isAntiAlias = true

                val clearingCircle = Path()
                clearingCircle.addCircle(
                    controlsCenter.x, controlsCenter.y, radius.times(0.55f), Path.Direction.CW
                )
                clearingCircle.close()

                val centerCircle = Path()
                centerCircle.addCircle(
                    controlsCenter.x, controlsCenter.y, radius.times(0.5f), Path.Direction.CW
                )
                centerCircle.close()
                canvas.drawPath(centerCircle, controlsPaint)

                val contrastColor =
                    if (ColorUtils.calculateLuminance(palette.getVibrantColor(Color.WHITE)) > 0.5f) Color.BLACK else Color.WHITE
                val centerIcon =
                    if (activeController!!.playbackState!!.isActive) pauseDrawable else playDrawable
                centerIcon?.setTint(contrastColor)
                centerIcon?.bounds = Rect(
                    (controlsCenter.x - radius.times(0.45f)).roundToInt(),
                    (controlsCenter.y - radius.times(0.45f)).roundToInt(),
                    (controlsCenter.x + radius.times(0.45f)).roundToInt(),
                    (controlsCenter.y + radius.times(0.45f)).roundToInt(),
                )
                centerIcon?.draw(canvas)

                val controlsRectPath = Path()
                controlsRectPath.addRoundRect(
                    RectF(
                        controlsCenter.x - radius.times(1.25f),
                        controlsCenter.y - radius.div(2f).times(0.85f),
                        controlsCenter.x + radius.times(1.25f),
                        controlsCenter.y + radius.div(2f).times(0.85f),
                    ), radius.div(6f), radius.div(6f), Path.Direction.CW
                )
                controlsRectPath.close()
                controlsRectPath.op(clearingCircle, Path.Op.DIFFERENCE)
                canvas.drawPath(controlsRectPath, controlsPaint)

                previousDrawable?.setTint(contrastColor)
                previousDrawable?.bounds = Rect(
                    (controlsCenter.x - radius.times(1.35f)).roundToInt(),
                    (controlsCenter.y - radius.times(0.45f)).roundToInt(),
                    (controlsCenter.x - radius.times(0.45f)).roundToInt(),
                    (controlsCenter.y + radius.times(0.45f)).roundToInt(),
                )
                previousDrawable?.draw(canvas)

                nextDrawable?.setTint(contrastColor)
                nextDrawable?.bounds = Rect(
                    (controlsCenter.x + radius.times(0.45f)).roundToInt(),
                    (controlsCenter.y - radius.times(0.45f)).roundToInt(),
                    (controlsCenter.x + radius.times(1.35f)).roundToInt(),
                    (controlsCenter.y + radius.times(0.45f)).roundToInt(),
                )
                nextDrawable?.draw(canvas)
            }
        }

        private fun draw() {
            val nextDraw = SystemClock.uptimeMillis() + (1000 / 24)
            val holder = surfaceHolder
            val canvas: Canvas?
            if (visible) {
                canvas = holder.lockHardwareCanvas()
                if (canvas != null) {
                    if (keyguardManager.isKeyguardLocked) {
                        clockCenter = PointF(width * 5f / 6f - 30f, width / 3f)
                        radius = width / 6f
                    } else {
                        clockCenter = PointF(width / 2f, width * 3f / 4f)
                        radius = width / 3f
                        mediaCenter = PointF(width / 2f, height / 2f + width / 4f)
                        controlsCenter = PointF(width / 2f, height / 2f + width / 2f + 20f)
                        setAlbumArt()
                    }
                    var palette = Palette.from(listOf(Palette.Swatch(Color.WHITE, 100)))
                    if (backgroundImage != null) {
                        canvas.save()
                        canvas.translate(if (!isPreview) width * xOffset else 0f, 0f)
                        drawImageCover(canvas, backgroundImage!!, width, height)
                        canvas.restore()
                        palette = Palette.from(backgroundImage!!).generate()
                    } else {
                        val blackPaint = Paint()
                        blackPaint.color = Color.BLACK
                        blackPaint.style = Paint.Style.FILL
                        canvas.drawRect(
                            RectF(0f, 0f, width.toFloat(), height.toFloat()), blackPaint
                        )
                    }
                    drawClock(canvas, clockCenter, radius, palette)
                    if (!keyguardManager.isKeyguardLocked) {
                        drawMediaPlayer(canvas, mediaCenter, radius / 2f, palette)
                    }
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postAtTime(drawRunner, nextDraw)
        }

    }
}