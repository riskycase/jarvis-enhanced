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
import android.media.session.PlaybackState
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
import com.riskycase.jarvisEnhanced.R
import com.riskycase.jarvisEnhanced.util.ClockConstants
import com.riskycase.jarvisEnhanced.util.NotificationListenerConnector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named
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
    private var albumArtBitmap: Bitmap? = null
        set(value) {
            // Recycle the old bitmap
            field?.recycle()

            value?.let { newBitmap ->
                val width = newBitmap.width
                val height = newBitmap.height
                val smaller = minOf(width, height)
                field = Bitmap.createBitmap(
                    newBitmap, (width - smaller) / 2, (height - smaller) / 2, smaller, smaller
                )

                // If the new bitmap was cropped, recycle the original
                if (field != newBitmap) {
                    newBitmap.recycle()
                }
            } ?: run {
                field = null
            }
        }

    @Inject
    lateinit var batteryManager: BatteryManager

    @Inject
    lateinit var keyguardManager: KeyguardManager

    @Inject
    lateinit var mediaSessionManager: MediaSessionManager

    @Inject
    lateinit var notificationListenerConnector: NotificationListenerConnector

    @Inject
    lateinit var clockConstants: ClockConstants

    @Inject
    @Named("OddDateFormat")
    lateinit var oddSimpleDateFormat: SimpleDateFormat

    @Inject
    @Named("EvenDateFormat")
    lateinit var evenSimpleDateFormat: SimpleDateFormat

    @Inject
    @Named("NotificationListenerServiceComponentName")
    lateinit var notificationListenerServiceComponentName: ComponentName

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

        private var mediaCenter: PointF = PointF(0f, 0f)
        private var controlsCenter: PointF = PointF(0f, 0f)
        private var activeController: MediaController? = null
        private val controllerCallbackMap = HashMap<String, MediaController.Callback>()
        private val clockTextPaint = Paint()
        private val shadowPaint = Paint()
        private var textBounds = Rect()

        private var playDrawable = getDrawable(R.drawable.play)
        private var pauseDrawable = getDrawable(R.drawable.pause)
        private var previousDrawable = getDrawable(R.drawable.previous)
        private var nextDrawable = getDrawable(R.drawable.next)

        init {
            handler.post(drawRunner)
            mediaSessionManager.addOnActiveSessionsChangedListener({ controllers ->
                activeController =
                    controllers?.find { controller -> controller.playbackState?.isActive == true }
                albumArtBitmap =
                    activeController?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                        ?: activeController?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                                ?: activeController?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                                ?: notificationListenerConnector.notificationListener?.getMediaNotificationByPackageName(
                            activeController?.packageName
                        )
                controllers?.forEach(this::registerCallbacksOnController)
            }, notificationListenerServiceComponentName)
            val controllers =
                mediaSessionManager.getActiveSessions(notificationListenerServiceComponentName)
            controllers.forEach(this::registerCallbacksOnController)
            activeController =
                controllers.find { controller -> controller.playbackState?.isActive == true }
            clockTextPaint.typeface = resources.getFont(R.font.dseg7modernmini)
            shadowPaint.color = Color.argb(64, 0, 0, 0)
            shadowPaint.style = Paint.Style.FILL
        }

        private fun registerCallbacksOnController(controller: MediaController) {
            if (!controllerCallbackMap.contains(controller.packageName)) {
                val callback = object : MediaController.Callback() {
                    override fun onSessionDestroyed() {
                        super.onSessionDestroyed()
                        controllerCallbackMap.remove(controller.packageName)
                        if (activeController == controller) activeController = null
                    }

                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        super.onPlaybackStateChanged(state)
                        if (state?.isActive == true) activeController = controller
                        albumArtBitmap?.recycle()
                        albumArtBitmap =
                            activeController?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                                ?: activeController?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                                        ?: activeController?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                                        ?: notificationListenerConnector.notificationListener?.getMediaNotificationByPackageName(
                                    activeController?.packageName
                                )

                    }

                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        super.onMetadataChanged(metadata)
                        if (activeController == controller) {
                            albumArtBitmap?.recycle()
                            albumArtBitmap =
                                metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                                            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                                            ?: notificationListenerConnector.notificationListener?.getMediaNotificationByPackageName(
                                        controller.packageName
                                    )
                        }
                    }
                }
                controllerCallbackMap[controller.packageName] = callback
                controller.registerCallback(callback, handler)
            }
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
                if (activeController != null) {
                    val mediaControlsRect = RectF(
                        controlsCenter.x - clockConstants.getClockRadius().div(2f).times(1.25f),
                        controlsCenter.y - clockConstants.getClockRadius().div(4f).times(0.85f),
                        controlsCenter.x + clockConstants.getClockRadius().div(2f).times(1.25f),
                        controlsCenter.y + clockConstants.getClockRadius().div(4f).times(0.85f),
                    )
                    if (((mediaCenter.x - x.toFloat()).pow(2) + (mediaCenter.y - y.toFloat()).pow(2)) < (clockConstants.getClockRadius()
                            .div(2f).pow(2))
                    ) {
                        activeController!!.sessionActivity?.send()
                    } else if (((controlsCenter.x - x.toFloat()).pow(2) + (controlsCenter.y - y.toFloat()).pow(
                            2
                        )) < (clockConstants.getClockRadius().div(4f).pow(2))
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

        private fun drawHand(
            canvas: Canvas,
            center: PointF,
            width: Float,
            height: Float,
            constant: Int,
            handPaint: Paint,
            radius: Float,
            shadowPaint: Paint
        ) {
            val rotation = clockConstants.getRotation(constant)
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
                clockTextPaint.textSize = radius / 6f
                val currentTimeText =
                    (if (now.get(Calendar.SECOND) % 2 == 0) evenSimpleDateFormat else oddSimpleDateFormat).format(
                        now
                    )
                clockTextPaint.getTextBounds(allOnString, 0, 8, textBounds)
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
                radius.minus(7.5f),
                shadowPaint
            )

            // Minutes hand
            drawHand(
                canvas,
                center,
                10f,
                radius.minus(7.5f),
                Calendar.MINUTE,
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
                Calendar.SECOND,
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

        private fun drawMediaPlayer(
            canvas: Canvas,
            center: PointF,
            radius: Float,
        ) {
            activeController?.also { activeController ->
                canvas.save()
                activeController.metadata?.getLong(
                    MediaMetadata.METADATA_KEY_DURATION
                )?.let {
                    (activeController.playbackState?.position?.toFloat())?.div(
                        it
                    )?.times(360f)
                }?.let {
                    canvas.rotate(
                        it, center.x, center.y
                    )
                }
                if (albumArtBitmap == null) albumArtBitmap =
                    activeController.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                        ?: activeController.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                                ?: activeController.metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                                ?: notificationListenerConnector.notificationListener?.getMediaNotificationByPackageName(
                            activeController.packageName
                        )
                albumArtBitmap?.let {
                    val albumArtPath = Path()
                    albumArtPath.addCircle(center.x, center.y, radius, Path.Direction.CW)
                    canvas.clipPath(albumArtPath)
                    canvas.drawBitmap(
                        it, null, RectF(
                            center.x - radius, center.y - radius,
                            center.x + radius, center.y + radius,
                        ), Paint()
                    )
                }
                canvas.restore()

                val controlsPaint = Paint()
                controlsPaint.color = baseColor
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

                val contrastColor = colorOnBaseColor
                val centerIcon =
                    if (activeController.playbackState?.isActive == true) pauseDrawable else playDrawable
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
            val nextDraw = SystemClock.uptimeMillis() + (1000 / 60)
            val holder = surfaceHolder
            val canvas: Canvas?
            clockConstants.updateVisibility(visible)
            if (visible) {
                canvas = holder.lockHardwareCanvas()
                if (canvas != null) {
                    if (!keyguardManager.isKeyguardLocked) {
                        mediaCenter = PointF(width / 2f, height / 2f + width / 4f)
                        controlsCenter = PointF(width / 2f, height / 2f + width / 2f + 20f)
                    }
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
                    drawClock(canvas)
                    if (!keyguardManager.isKeyguardLocked) {
                        drawMediaPlayer(canvas, mediaCenter, width / 6f)
                    }
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postAtTime(drawRunner, nextDraw)
        }

    }
}