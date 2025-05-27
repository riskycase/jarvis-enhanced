package com.riskycase.jarvisEnhanced.util.wallpaper

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
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
import android.os.BatteryManager
import android.provider.AlarmClock
import com.riskycase.jarvisEnhanced.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class ClockUtils @Inject constructor(
    private val keyguardManager: KeyguardManager,
    @ApplicationContext private val applicationContext: Context
) {

    enum class ClockCenterStates {
        KEYGUARD_LOCKED, KEYGUARD_UNLOCKED
    }

    enum class ClockRadiusStates {
        KEYGUARD_LOCKED, KEYGUARD_UNLOCKED, NOT_VISIBLE
    }

    private var width = 0
    private var height = 0
    private var textPaint = Paint()
        get() {
            field.textSize = clockRadiusAnimator.getValue(System.currentTimeMillis()) / 6f
            return field
        }
    private var textBounds = Rect()
        get() {
            textPaint.getTextBounds(allOnString, 0, 8, field)
            return field
        }
    private val clockShadowPaint = Paint()

    private var previousCenterState =
        if (keyguardManager.isKeyguardLocked) ClockCenterStates.KEYGUARD_LOCKED else ClockCenterStates.KEYGUARD_UNLOCKED
    private var previousVisibleState = ClockRadiusStates.NOT_VISIBLE

    private val clockCenterAnimator =
        AnimatorPointF(keyguardManager.isKeyguardLocked, PointF(0f, 0f))
    private var unlockedClockCenter = PointF(width / 2f, width * 3f / 4f)
    private var lockedClockCenter = PointF(width * 5f / 6f - 30f, width / 3f)

    private val clockRadiusAnimator = AnimatorFloat(ClockRadiusStates.NOT_VISIBLE, 0f)
    private var notVisibleRadius = width * 3f
    private var unlockedVisibleRadius = width / 3f
    private var lockedVisibleRadius = width / 6f

    private val hourHandRotationAnimator = AnimatorFloat(ClockRadiusStates.NOT_VISIBLE, 0f)
    private val minuteHandRotationAnimator = AnimatorFloat(ClockRadiusStates.NOT_VISIBLE, 0f)
    private val secondHandRotationAnimator = AnimatorFloat(ClockRadiusStates.NOT_VISIBLE, 0f)

    private val lockUnlockAnimationDuration = 1000L
    private val clockVisibilityAnimationDuration = 1000L
    private val clockSweepAnimationDuration = 1000L

    @Inject
    lateinit var batteryManager: BatteryManager

    @Inject
    lateinit var backgroundImageUtils: BackgroundImageUtils

    @Inject
    @Named("OddDateFormat")
    lateinit var oddSimpleDateFormat: SimpleDateFormat

    @Inject
    @Named("EvenDateFormat")
    lateinit var evenSimpleDateFormat: SimpleDateFormat

    @Inject
    @Named("7SegmentAllOnString")
    lateinit var allOnString: String

    init {
        textPaint.typeface = applicationContext.resources.getFont(R.font.dseg7modernmini)
        clockShadowPaint.color = Color.argb(64, 0, 0, 0)
        clockShadowPaint.style = Paint.Style.FILL
    }

    private fun getHandRotations(calendar: Calendar, value: Int): Float {
        return when (value) {
            Calendar.HOUR -> ((calendar.get(Calendar.HOUR)
                .toFloat() / 12f) + (calendar.get(Calendar.MINUTE)
                .toFloat() / 720f) + (calendar.get(
                Calendar.SECOND
            ).toFloat() / 43200f) + calendar.get(Calendar.MILLISECOND).toFloat() / 43200000f) * 360f

            Calendar.MINUTE -> ((calendar.get(Calendar.MINUTE).toFloat() / 60f) + (calendar.get(
                Calendar.SECOND
            ).toFloat() / 3600f) + calendar.get(
                Calendar.MILLISECOND
            ).toFloat() / 3600000f) * 360f

            Calendar.SECOND -> ((calendar.get(Calendar.SECOND).toFloat() / 60f) + calendar.get(
                Calendar.MILLISECOND
            ).toFloat() / 60000f) * 360f

            else -> 0f
        }
    }

    fun updateDimensions(width: Int, height: Int) {
        this.width = width
        this.height = height

        unlockedClockCenter = PointF(width / 2f, width * 3f / 4f)
        lockedClockCenter = PointF(width * 5f / 6f - 30f, width / 3f)
        clockCenterAnimator.forceValue(if (keyguardManager.isKeyguardLocked) lockedClockCenter else unlockedClockCenter)

        notVisibleRadius = width * 3f
        unlockedVisibleRadius = width / 3f
        lockedVisibleRadius = width / 6f
        clockRadiusAnimator.forceValue(if (keyguardManager.isKeyguardLocked) lockedVisibleRadius else unlockedVisibleRadius)
    }

    fun updateVisibility(currentlyVisible: Boolean) {
        val currentVisibleState =
            if (!currentlyVisible) ClockRadiusStates.NOT_VISIBLE else if (keyguardManager.isKeyguardLocked) ClockRadiusStates.KEYGUARD_LOCKED else ClockRadiusStates.KEYGUARD_UNLOCKED
        if (currentVisibleState != previousVisibleState) {
            when (currentVisibleState) {
                ClockRadiusStates.NOT_VISIBLE -> clockRadiusAnimator.forceValue(notVisibleRadius)
                ClockRadiusStates.KEYGUARD_LOCKED -> clockRadiusAnimator.forceValue(
                    lockedVisibleRadius
                )
                else -> {
                    clockRadiusAnimator.setAnimation(
                        System.currentTimeMillis(),
                        clockVisibilityAnimationDuration,
                        notVisibleRadius,
                        unlockedVisibleRadius
                    )
                    val finalCalendar = Calendar.getInstance()
                    val hourRotation = getHandRotations(finalCalendar, Calendar.HOUR)
                    val minuteRotation = getHandRotations(finalCalendar, Calendar.MINUTE)
                    val secondRotation = getHandRotations(finalCalendar, Calendar.SECOND)
                    finalCalendar.add(
                        Calendar.MILLISECOND,
                        (lockUnlockAnimationDuration + clockSweepAnimationDuration).toInt()
                    )
                    hourHandRotationAnimator.setAnimation(
                        System.currentTimeMillis() + lockUnlockAnimationDuration,
                        clockSweepAnimationDuration,
                        if (hourRotation > 180f) 0f else 360f,
                        hourRotation
                    )
                    minuteHandRotationAnimator.setAnimation(
                        System.currentTimeMillis() + lockUnlockAnimationDuration,
                        clockSweepAnimationDuration,
                        if (minuteRotation > 180f) 0f else 360f,
                        minuteRotation
                    )
                    secondHandRotationAnimator.setAnimation(
                        System.currentTimeMillis() + lockUnlockAnimationDuration,
                        clockSweepAnimationDuration,
                        if (secondRotation > 180f) 0f else 360f,
                        secondRotation
                    )
                }
            }
            previousVisibleState = currentVisibleState
        }
    }

    private fun getClockCenter(): PointF {
        val currentCenterState =
            if (keyguardManager.isKeyguardLocked) ClockCenterStates.KEYGUARD_LOCKED else ClockCenterStates.KEYGUARD_UNLOCKED
        if (currentCenterState != previousCenterState) {
            if (currentCenterState == ClockCenterStates.KEYGUARD_LOCKED) {
                clockCenterAnimator.forceValue(lockedClockCenter)
            } else {
                clockCenterAnimator.setAnimation(
                    System.currentTimeMillis(),
                    lockUnlockAnimationDuration,
                    lockedClockCenter,
                    unlockedClockCenter
                )
            }
            previousCenterState = currentCenterState
        }
        return clockCenterAnimator.getValue(System.currentTimeMillis())
    }

    private fun getClockRadius(): Float {
        val currentVisibleState =
            if (keyguardManager.isKeyguardLocked) ClockRadiusStates.KEYGUARD_LOCKED else ClockRadiusStates.KEYGUARD_UNLOCKED
        if (currentVisibleState != previousVisibleState) {
            if (currentVisibleState == ClockRadiusStates.KEYGUARD_LOCKED) clockRadiusAnimator.forceValue(
                lockedVisibleRadius
            )
            else clockRadiusAnimator.setAnimation(
                System.currentTimeMillis(),
                clockVisibilityAnimationDuration,
                notVisibleRadius,
                unlockedVisibleRadius
            )
        }
        return clockRadiusAnimator.getValue(
            System.currentTimeMillis()
        )
    }

    private fun getRotation(value: Int): Float {
        val calendar = Calendar.getInstance()
        val finalRotation = when (value) {
            Calendar.HOUR -> ((calendar.get(Calendar.HOUR)
                .toFloat() / 12f) + (calendar.get(Calendar.MINUTE)
                .toFloat() / 720f) + (calendar.get(
                Calendar.SECOND
            ).toFloat() / 43200f) + calendar.get(Calendar.MILLISECOND).toFloat() / 43200000f) * 360f

            Calendar.MINUTE -> ((calendar.get(Calendar.MINUTE).toFloat() / 60f) + (calendar.get(
                Calendar.SECOND
            ).toFloat() / 3600f) + calendar.get(
                Calendar.MILLISECOND
            ).toFloat() / 3600000f) * 360f

            Calendar.SECOND -> ((calendar.get(Calendar.SECOND).toFloat() / 60f) + calendar.get(
                Calendar.MILLISECOND
            ).toFloat() / 60000f) * 360f

            else -> 0f
        }
        val currentVisibleState =
            if (keyguardManager.isKeyguardLocked) ClockRadiusStates.KEYGUARD_LOCKED else ClockRadiusStates.KEYGUARD_UNLOCKED
        if (currentVisibleState != previousVisibleState) {
            if (currentVisibleState == ClockRadiusStates.KEYGUARD_LOCKED) {
                (when (value) {
                    Calendar.HOUR -> hourHandRotationAnimator
                    Calendar.MINUTE -> minuteHandRotationAnimator
                    else -> secondHandRotationAnimator
                }).forceValue(finalRotation)
            } else {
                val finalCalendar = Calendar.getInstance()
                finalCalendar.add(
                    Calendar.MILLISECOND,
                    (lockUnlockAnimationDuration + clockSweepAnimationDuration).toInt()
                )
                (when (value) {
                    Calendar.HOUR -> hourHandRotationAnimator
                    Calendar.MINUTE -> minuteHandRotationAnimator
                    else -> secondHandRotationAnimator
                }).setAnimation(
                    System.currentTimeMillis() + lockUnlockAnimationDuration,
                    lockUnlockAnimationDuration,
                    0f,
                    getHandRotations(finalCalendar, value)
                )
            }
        }
        return (when (value) {
            Calendar.HOUR -> hourHandRotationAnimator
            Calendar.MINUTE -> minuteHandRotationAnimator
            else -> secondHandRotationAnimator
        }).getValue(System.currentTimeMillis(), finalRotation)
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
        val rotation = getRotation(constant)
        val shadowPaint = clockShadowPaint
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

    fun drawClock(canvas: Canvas) {

        val center = getClockCenter()
        val radius = getClockRadius()

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
        bodyPaint.color = backgroundImageUtils.getBaseColor()
        bodyPaint.style = Paint.Style.FILL

        // Main face
        canvas.drawCircle(center.x, center.y, radius.minus(7.5f), bodyPaint)

        bodyPaint.style = Paint.Style.STROKE
        bodyPaint.strokeWidth = 10f
        // Border outline
        canvas.drawCircle(center.x, center.y, radius.plus(7.5f), bodyPaint)

        val now = Calendar.getInstance()
        val contrastColor = backgroundImageUtils.getColorOnBaseColor()

        if (!keyguardManager.isKeyguardLocked) {
            val currentTimeText =
                (if (now.get(Calendar.SECOND) % 2 == 0) evenSimpleDateFormat else oddSimpleDateFormat).format(
                    now
                )
            textPaint.color = backgroundImageUtils.getDarkerBaseColor()
            canvas.drawRect(
                RectF(
                    center.x - (textBounds.width().toFloat() / 2f) - 20f,
                    center.y - (radius / 2f) - (textBounds.height().toFloat()) + 10f,
                    center.x + (textBounds.width().toFloat() / 2f) + 17.5f,
                    center.y - (radius / 2f) + 47.5f
                ), textPaint
            )
            textPaint.color = backgroundImageUtils.getDarkBaseColor()
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
                allOnString,
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

        // Hours hand
        drawHand(
            canvas, center, 15f, radius.times(0.65f), Calendar.HOUR, clockPaint, radius.minus(7.5f)
        )

        // Minutes hand
        drawHand(
            canvas, center, 10f, radius.minus(7.5f), Calendar.MINUTE, clockPaint, radius.minus(7.5f)
        )

        // Seconds hand
        drawHand(
            canvas, center, 4f, radius.minus(7.5f), Calendar.SECOND, clockPaint, radius.minus(7.5f)
        )

        // Center piece
        bodyPaint.style = Paint.Style.FILL
        canvas.drawCircle(center.x, center.y, 25f, bodyPaint)
        clockPaint.style = Paint.Style.STROKE
        clockPaint.strokeWidth = 7.5f
        canvas.drawCircle(center.x, center.y, 25f, clockPaint)
    }

    fun handleTouchEvent(x: Int, y: Int) {
        if (((getClockCenter().x - x.toFloat()).pow(2) + (getClockCenter().y - y.toFloat()).pow(2)) < (getClockRadius()
                .pow(2))
        ) {
            val openAlarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            openAlarmIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            applicationContext.startActivity(openAlarmIntent)
        }
    }

}