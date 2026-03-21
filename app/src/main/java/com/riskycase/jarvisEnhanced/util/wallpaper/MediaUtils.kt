package com.riskycase.jarvisEnhanced.util.wallpaper

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
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
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.riskycase.jarvisEnhanced.R
import com.riskycase.jarvisEnhanced.util.SocketChannels.MUSIC_ALBUM_ART_DETAILS
import com.riskycase.jarvisEnhanced.util.SocketChannels.MUSIC_DETAILS
import com.riskycase.jarvisEnhanced.util.SocketChannels.MusicCommands.NEXT
import com.riskycase.jarvisEnhanced.util.SocketChannels.MusicCommands.PAUSE
import com.riskycase.jarvisEnhanced.util.SocketChannels.MusicCommands.PLAY
import com.riskycase.jarvisEnhanced.util.SocketChannels.MusicCommands.PREVIOUS
import com.riskycase.jarvisEnhanced.util.SocketChannels.MusicCommands.SEEK_TO
import com.riskycase.jarvisEnhanced.util.SocketChannels.MusicDetails
import com.riskycase.jarvisEnhanced.util.SocketIOTransport
import com.riskycase.jarvisEnhanced.util.SystemServicesContainer
import dagger.hilt.android.qualifiers.ApplicationContext

import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.core.graphics.withSave

@Singleton
class MediaUtils @Inject constructor(
    private val mediaSessionManager: MediaSessionManager,
    private val packageManager: PackageManager,
    @ApplicationContext private val applicationContext: Context,
    @Named("NotificationListenerServiceComponentName") private val notificationListenerServiceComponentName: ComponentName,
    private val systemServicesContainer: SystemServicesContainer,
    private val socketIOTransport: SocketIOTransport,
    private val gson: Gson
) {

    val handler = Handler(Looper.getMainLooper())

    private val musicDetailsMap: MutableMap<String, String> = HashMap()

    private val albumArtLock = Any()

    @Volatile
    private var albumArtBitmap: Bitmap? = null
        set(value) {
            synchronized(albumArtLock) {
                // Only process if the new value is not null and not recycled
                value?.let { newBitmap ->
                    if (!newBitmap.isRecycled) {
                        val oldValue = field
                        val width = newBitmap.width
                        val height = newBitmap.height
                        val smaller = minOf(width, height)
                        
                        try {
                            field = Bitmap.createBitmap(
                                newBitmap,
                                (width - smaller) / 2,
                                (height - smaller) / 2,
                                smaller,
                                smaller
                            )

                            // If the new bitmap was cropped, recycle the original
                            if (field != newBitmap && !newBitmap.isRecycled) {
                                newBitmap.recycle()
                            }
                            
                            // Safely recycle the old bitmap
                            oldValue?.let { old ->
                                if (!old.isRecycled) {
                                    old.recycle()
                                }
                            }
                        } catch (e: IllegalArgumentException) {
                            // Handle case where bitmap parameters are invalid
                            // Keep the old bitmap in this case
                        } catch (e: RuntimeException) {
                            // Handle other bitmap-related exceptions
                            // Keep the old bitmap in this case
                        }
                    }
                } ?: run {
                    // If value is null, just recycle the old bitmap
                    field?.let { old ->
                        if (!old.isRecycled) {
                            old.recycle()
                        }
                    }
                    field = null
                }
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

    @Inject
    lateinit var backgroundImageUtils: BackgroundImageUtils

    @Inject
    lateinit var keyguardManager: KeyguardManager

    init {
        if (hasPermissions()) {
            setup()
        }
    }

    private fun hasPermissions(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(applicationContext)
            .contains(applicationContext.packageName)
    }

    fun setup() {
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
        controllers?.forEach(this::setDetailsFromController)
        socketIOTransport.sendMessage(MUSIC_DETAILS, musicDetailsMap)
        setAlbumArtFromController(activeController)
    }

    fun setAlbumArtFromController(controller: MediaController?) {
        albumArtBitmap = getAlbumArtFromController(controller)
    }

    fun handleSocketCommand(parts: List<String>) {
        mediaSessionManager.getActiveSessions(notificationListenerServiceComponentName)
            .first { controller -> controller.packageName == parts[2] }.let {
                when (parts[3]) {
                    PREVIOUS -> it.transportControls.skipToPrevious()
                    PAUSE -> it.transportControls.pause()
                    PLAY -> it.transportControls.play()
                    NEXT -> it.transportControls.skipToNext()
                    SEEK_TO -> it.transportControls.seekTo(parts[4].toLong())
                }
            }
    }

    private fun getAlbumArtFromController(controller: MediaController?): Bitmap? {
        return controller?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: controller?.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: systemServicesContainer.notificationListener?.getMediaNotificationByPackageName(
                controller?.packageName
            )
    }

    private fun registerCallbacksOnController(controller: MediaController) {
        if (!controllerCallbackMap.contains(controller.packageName)) {
            val callback = object : MediaController.Callback() {
                override fun onSessionDestroyed() {
                    super.onSessionDestroyed()
                    controllerCallbackMap.remove(controller.packageName)
                    musicDetailsMap.remove(controller.packageName)
                    socketIOTransport.sendMessage(MUSIC_DETAILS, musicDetailsMap)
                    if (activeController == controller) activeController = null
                }

                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    super.onPlaybackStateChanged(state)
                    if (state?.isActive == true) activeController = controller
                    updatePlaybackState(controller)
                    socketIOTransport.sendMessage(MUSIC_DETAILS, musicDetailsMap)
                    setAlbumArtFromController(activeController)
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    super.onMetadataChanged(metadata)
                    setDetailsFromController(controller)
                    socketIOTransport.sendMessage(MUSIC_DETAILS, musicDetailsMap)
                    if (activeController == controller) {
                        setAlbumArtFromController(controller)
                    }
                    handler.postDelayed({
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        getAlbumArtFromController(controller)?.also { bitmap ->
                            // Check if bitmap is recycled before compressing
                            if (!bitmap.isRecycled) {
                                try {
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                                        .also {
                                            socketIOTransport.sendMessage(
                                                MUSIC_ALBUM_ART_DETAILS, mapOf(
                                                    Pair(
                                                        controller.packageName, Base64.encodeToString(
                                                            byteArrayOutputStream.toByteArray(),
                                                            Base64.NO_WRAP
                                                        )
                                                    )
                                                )
                                            )
                                        }
                                } catch (e: IllegalStateException) {
                                    // Handle case where bitmap gets recycled during compression
                                    // This can happen in rare race conditions
                                }
                            }
                        }
                    }, 450) // Needed to give Poweramp space to update art
                }
            }
            controllerCallbackMap[controller.packageName] = callback
            controller.registerCallback(callback)
        }
    }

    private fun updatePlaybackState(controller: MediaController) {
        val map = gson.fromJson<HashMap<String, String>>(
            musicDetailsMap[controller.packageName], HashMap::class.java
        )
        map[MusicDetails.MUSIC_UPDATED_AT] =
            controller.playbackState?.lastPositionUpdateTime?.plus((System.currentTimeMillis() - SystemClock.elapsedRealtime()))
                .toString()
        map[MusicDetails.MUSIC_PLAYING_STATE] = controller.playbackState?.state.toString()
        map[MusicDetails.MUSIC_POSITION] = controller.playbackState?.position.toString()
        musicDetailsMap[controller.packageName] = gson.toJson(map)
    }

    private fun setDetailsFromController(controller: MediaController) {
        musicDetailsMap[controller.packageName] = gson.toJson(
            mapOf(
                Pair(
                    MusicDetails.MUSIC_PLAYING_STATE, controller.playbackState?.state
                ), Pair(
                    MusicDetails.MUSIC_TITLE,
                    controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ), Pair(
                    MusicDetails.MUSIC_ALBUM,
                    controller.metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
                ), Pair(
                    MusicDetails.MUSIC_ARTIST,
                    controller.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ), Pair(
                    MusicDetails.MUSIC_DURATION,
                    controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
                ), Pair(
                    MusicDetails.MUSIC_POSITION, controller.playbackState?.position
                ), Pair(
                    MusicDetails.MUSIC_UPDATED_AT,
                    controller.playbackState?.lastPositionUpdateTime?.plus((System.currentTimeMillis() - SystemClock.elapsedRealtime()))
                ), Pair(
                    MusicDetails.MUSIC_PLAYER_NAME, packageManager.getApplicationInfo(
                        controller.packageName, PackageManager.MATCH_ALL
                    ).loadLabel(packageManager)
                )
            )
        )
    }

    fun sendFullUpdate() {
        if (hasPermissions()) {
            mediaSessionManager.getActiveSessions(notificationListenerServiceComponentName)
                .forEach {
                    setDetailsFromController(it)
                }
            socketIOTransport.sendMessage(MUSIC_DETAILS, musicDetailsMap)
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
            canvas.withSave {
                activeController.metadata?.getLong(
                    MediaMetadata.METADATA_KEY_DURATION
                )?.let {
                    (activeController.playbackState?.position?.toFloat())?.div(
                        it
                    )?.times(360f)
                }?.let {
                    rotate(
                        it, mediaCenter.x, mediaCenter.y
                    )
                }
                val albumArtBackgroundPaint = Paint()
                albumArtBackgroundPaint.color = backgroundImageUtils.getBaseColor()
                albumArtBackgroundPaint.style = Paint.Style.FILL
                albumArtBackgroundPaint.isAntiAlias = true
                drawCircle(
                    mediaCenter.x, mediaCenter.y, mediaRadius - 5, albumArtBackgroundPaint
                )
                val albumArtPath = Path()
                albumArtPath.addCircle(
                    mediaCenter.x, mediaCenter.y, mediaRadius, Path.Direction.CW
                )
                clipPath(albumArtPath)
                if (albumArtBitmap == null) setAlbumArtFromController(activeController)

                synchronized(albumArtLock) {
                    albumArtBitmap?.let { bitmap ->
                        // Check if bitmap is recycled before drawing
                        if (!bitmap.isRecycled) {
                            try {
                                drawBitmap(
                                    bitmap, null, RectF(
                                        mediaCenter.x - mediaRadius,
                                        mediaCenter.y - mediaRadius,
                                        mediaCenter.x + mediaRadius,
                                        mediaCenter.y + mediaRadius,
                                    ), Paint()
                                )
                            } catch (e: RuntimeException) {
                                // Handle case where bitmap gets recycled during drawing
                                // This can happen in rare race conditions
                            }
                        }
                    }
                }
            }
        }
    }

    private fun drawMediaControls(canvas: Canvas) {

        val controlsPaint = Paint()
        controlsPaint.color = backgroundImageUtils.getBaseColor()
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

        pauseDrawable?.setTint(backgroundImageUtils.getColorOnBaseColor())
        playDrawable?.setTint(backgroundImageUtils.getColorOnBaseColor())
        (if (activeController?.playbackState?.isActive == true) pauseDrawable else playDrawable)?.draw(
            canvas
        )

        previousDrawable?.setTint(backgroundImageUtils.getColorOnBaseColor())
        previousDrawable?.draw(canvas)

        nextDrawable?.setTint(backgroundImageUtils.getColorOnBaseColor())
        nextDrawable?.draw(canvas)
    }

    fun drawMediaPlayer(canvas: Canvas) {
        if (!keyguardManager.isKeyguardLocked) activeController?.also {
            drawAlbumArt(canvas)
            drawMediaControls(canvas)
        }
    }

    fun handleTouchInput(x: Int, y: Int) {
        activeController?.also { activeController ->
            if (((mediaCenter.x - x.toFloat()).pow(2) + (mediaCenter.y - y.toFloat()).pow(2)) < (mediaRadius.pow(
                    2
                ))
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    activeController.sessionActivity?.send(
                        ActivityOptions.makeBasic()
                            .setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            )
                            .setPendingIntentCreatorBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            )
                            .toBundle()
                    )
                } else {
                    activeController.sessionActivity?.send()
                }
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