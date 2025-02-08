package com.riskycase.jarvisEnhanced.util

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.riskycase.jarvisEnhanced.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.pow
import kotlin.math.roundToInt

class MediaUtils @Inject constructor(
    mediaSessionManager: MediaSessionManager,
    @Named("NotificationListenerServiceComponentName") private val notificationListenerServiceComponentName: ComponentName,
    private val notificationListenerConnector: NotificationListenerConnector,
    @ApplicationContext applicationContext: Context
) {

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

    private var mediaCenter = PointF(0f, 0f)
    private var controlsCenter = PointF(0f, 0f)
    private var mediaRadius = 0f
    private var mediaControlsRect = RectF(0f, 0f, 0f, 0f)

    private var playDrawable = applicationContext.getDrawable(R.drawable.play)
    private var pauseDrawable = applicationContext.getDrawable(R.drawable.pause)

    private var previousDrawable = applicationContext.getDrawable(R.drawable.previous)

    private var nextDrawable = applicationContext.getDrawable(R.drawable.next)

    private var activeController: MediaController? = null
    private val controllerCallbackMap = HashMap<String, MediaController.Callback>()

    init {
        mediaSessionManager.addOnActiveSessionsChangedListener(
            this::controllersUtilityFunction, notificationListenerServiceComponentName
        )
        this.controllersUtilityFunction(
            mediaSessionManager.getActiveSessions(
                notificationListenerServiceComponentName
            )
        )
    }

    private fun controllersUtilityFunction(controllers: List<MediaController>?) {
        activeController =
            controllers?.find { controller -> controller.playbackState?.isActive == true }
        controllers?.forEach(this::registerCallbacksOnController)
        setAlbumArtFromController(activeController)
    }

    fun setAlbumArtFromController(controller: MediaController?) {
        albumArtBitmap = controller?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: controller?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: notificationListenerConnector.notificationListener?.getMediaNotificationByPackageName(
                controller?.packageName
            )
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
                    setAlbumArtFromController(activeController)
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    super.onMetadataChanged(metadata)
                    if (activeController == controller) {
                        setAlbumArtFromController(controller)
                    }
                }
            }
            controllerCallbackMap[controller.packageName] = callback
            controller.registerCallback(callback)
        }
    }

    fun updateDimensions(width: Int, height: Int) {
        mediaCenter = PointF(width / 2f, height / 2f + width / 4f)
        controlsCenter = PointF(width / 2f, height / 2f + width / 2f + 20f)
        mediaRadius = width / 6f
        mediaControlsRect = RectF(
            controlsCenter.x - mediaRadius.times(1.25f),
            controlsCenter.y - mediaRadius.times(0.425f),
            controlsCenter.x + mediaRadius.times(1.25f),
            controlsCenter.y + mediaRadius.times(0.425f)
        )
        playDrawable?.bounds = Rect(
            (controlsCenter.x - mediaRadius.times(0.45f)).roundToInt(),
            (controlsCenter.y - mediaRadius.times(0.45f)).roundToInt(),
            (controlsCenter.x + mediaRadius.times(0.45f)).roundToInt(),
            (controlsCenter.y + mediaRadius.times(0.45f)).roundToInt(),
        )
        pauseDrawable?.bounds = Rect(
            (controlsCenter.x - mediaRadius.times(0.45f)).roundToInt(),
            (controlsCenter.y - mediaRadius.times(0.45f)).roundToInt(),
            (controlsCenter.x + mediaRadius.times(0.45f)).roundToInt(),
            (controlsCenter.y + mediaRadius.times(0.45f)).roundToInt(),
        )
        previousDrawable?.bounds = Rect(
            (controlsCenter.x - mediaRadius.times(1.35f)).roundToInt(),
            (controlsCenter.y - mediaRadius.times(0.45f)).roundToInt(),
            (controlsCenter.x - mediaRadius.times(0.45f)).roundToInt(),
            (controlsCenter.y + mediaRadius.times(0.45f)).roundToInt(),
        )
        nextDrawable?.bounds = Rect(
            (controlsCenter.x + mediaRadius.times(0.45f)).roundToInt(),
            (controlsCenter.y - mediaRadius.times(0.45f)).roundToInt(),
            (controlsCenter.x + mediaRadius.times(1.35f)).roundToInt(),
            (controlsCenter.y + mediaRadius.times(0.45f)).roundToInt(),
        )
    }

    private fun drawAlbumArt(canvas: Canvas) {
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
                    it, mediaCenter.x, mediaCenter.y
                )
            }
            if (albumArtBitmap == null) setAlbumArtFromController(activeController)
            albumArtBitmap?.let {
                val albumArtPath = Path()
                albumArtPath.addCircle(
                    mediaCenter.x, mediaCenter.y, mediaRadius, Path.Direction.CW
                )
                canvas.clipPath(albumArtPath)
                canvas.drawBitmap(
                    it, null, RectF(
                        mediaCenter.x - mediaRadius,
                        mediaCenter.y - mediaRadius,
                        mediaCenter.x + mediaRadius,
                        mediaCenter.y + mediaRadius,
                    ), Paint()
                )
                canvas.restore()
            }
        }
    }

    private fun drawMediaControls(canvas: Canvas, baseColor: Int, tintColor: Int) {

        val controlsPaint = Paint()
        controlsPaint.color = baseColor
        controlsPaint.style = Paint.Style.FILL
        controlsPaint.isAntiAlias = true

        val clearingCircle = Path()
        clearingCircle.addCircle(
            controlsCenter.x, controlsCenter.y, mediaRadius.times(0.55f), Path.Direction.CW
        )
        clearingCircle.close()

        val centerCircle = Path()
        centerCircle.addCircle(
            controlsCenter.x, controlsCenter.y, mediaRadius.times(0.5f), Path.Direction.CW
        )
        centerCircle.close()
        canvas.drawPath(centerCircle, controlsPaint)

        val controlsRectPath = Path()
        controlsRectPath.addRoundRect(
            mediaControlsRect, mediaRadius.div(6f), mediaRadius.div(6f), Path.Direction.CW
        )
        controlsRectPath.close()
        controlsRectPath.op(clearingCircle, Path.Op.DIFFERENCE)
        canvas.drawPath(controlsRectPath, controlsPaint)

        pauseDrawable?.setTint(tintColor)
        playDrawable?.setTint(tintColor)
        (if (activeController?.playbackState?.isActive == true) pauseDrawable else playDrawable)?.draw(
            canvas
        )

        previousDrawable?.setTint(tintColor)
        previousDrawable?.draw(canvas)

        nextDrawable?.setTint(tintColor)
        nextDrawable?.draw(canvas)
    }

    fun drawMediaPlayer(canvas: Canvas, baseColor: Int, tintColor: Int) {
        drawAlbumArt(canvas)
        drawMediaControls(canvas, baseColor, tintColor)
    }

    fun handleTouchInput(x: Int, y: Int) {
        activeController?.also { activeController ->
            if (((mediaCenter.x - x.toFloat()).pow(2) + (mediaCenter.y - y.toFloat()).pow(2)) < (mediaRadius.pow(
                    2
                ))
            ) {
                activeController.sessionActivity?.send()
            } else if (((controlsCenter.x - x.toFloat()).pow(2) + (controlsCenter.y - y.toFloat()).pow(
                    2
                )) < (mediaRadius.times(0.5f).pow(2))
            ) {
                if (activeController.playbackState!!.isActive) {
                    activeController.transportControls.pause()
                } else {
                    activeController.transportControls.play()
                }
            } else if (mediaControlsRect.contains(x.toFloat(), y.toFloat())) {
                if (x < mediaControlsRect.centerX()) {
                    activeController.transportControls.skipToPrevious()
                } else {
                    activeController.transportControls.skipToNext()
                }
            }
        }
    }

}