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
import androidx.core.graphics.withClip
import com.riskycase.jarvisEnhanced.R
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Module
@InstallIn(ViewComponent::class)
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
    private val batteryPaint = Paint()
    private val bodyPaint = Paint()
    private val clockPaint = Paint()
    private val clipPath = Path()
    private val donutClip = Path()
    private val scratchPath = Path()
    private val gradientMatrix = Matrix()
    private val scratchRect = RectF()
    private val innerClipRect = RectF()
    private val outerClipRect = RectF()
    private val frameCalendar: Calendar = Calendar.getInstance()

    // Cached geometry — rebuilt only when center/radius change
    private var lastCenter = PointF(Float.NaN, Float.NaN)
    private var lastRadius = Float.NaN
    private var innerFaceRadius = 0f
    private var outerFaceRadius = 0f
    private var cdr = 0f

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
        batteryPaint.style = Paint.Style.STROKE
        bodyPaint.style = Paint.Style.FILL
        clockPaint.style = Paint.Style.FILL
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
                    finalCalendar.add(
                        Calendar.MILLISECOND,
                        (lockUnlockAnimationDuration + clockSweepAnimationDuration).toInt()
                    )
                    val hourRotation = getHandRotations(finalCalendar, Calendar.HOUR)
                    val minuteRotation = getHandRotations(finalCalendar, Calendar.MINUTE)
                    val secondRotation = getHandRotations(finalCalendar, Calendar.SECOND)
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

    private fun getRotation(calendar: Calendar, value: Int): Float {
        val finalRotation = getHandRotations(calendar, value)
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
                val savedTime = calendar.timeInMillis
                calendar.add(
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
                    getHandRotations(calendar, value)
                )
                calendar.timeInMillis = savedTime
            }
        }
        return (when (value) {
            Calendar.HOUR -> hourHandRotationAnimator
            Calendar.MINUTE -> minuteHandRotationAnimator
            else -> secondHandRotationAnimator
        }).getValue(System.currentTimeMillis(), finalRotation)
    }

    private fun getHandGeometry(
        calendar: Calendar,
        width: Float,
        height: Float,
        constant: Int
    ): Triple<PointF, PointF, PointF> {
        val rotation = getRotation(calendar, constant)
        val sinValue = sin(rotation * PI / 180f).toFloat()
        val cosValue = cos(rotation * PI / 180f).toFloat()
        return Triple(
            PointF(width.times(0.5f) * -cosValue, width.times(0.5f) * -sinValue),
            PointF(height * sinValue, height * -cosValue),
            PointF(width * cosValue, width * sinValue)
        )
    }

    private fun drawHandShape(
        canvas: Canvas,
        center: PointF,
        start: PointF,
        heightOutwards: PointF,
        widthCW: PointF,
        paint: Paint
    ) {
        val handPath = scratchPath
        handPath.reset()
        handPath.moveTo(center.x + start.x, center.y + start.y)
        handPath.rLineTo(heightOutwards.x, heightOutwards.y)
        handPath.rLineTo(widthCW.x, widthCW.y)
        handPath.rLineTo(-heightOutwards.x, -heightOutwards.y)
        handPath.close()
        canvas.drawPath(handPath, paint)
    }

    private fun drawHandShadow(
        canvas: Canvas,
        center: PointF,
        start: PointF,
        heightOutwards: PointF,
        widthCW: PointF,
        radius: Float
    ) {
        val so = radius.times(2f)
        val sx = start.x
        val sy = start.y
        val hx = heightOutwards.x
        val hy = heightOutwards.y
        val wx = widthCW.x
        val wy = widthCW.y

        val shadowPath = scratchPath
        shadowPath.reset()
        shadowPath.moveTo(center.x + sx, center.y + sy)
        shadowPath.lineTo(center.x + sx + hx, center.y + sy + hy)
        shadowPath.lineTo(center.x + sx + hx + so, center.y + sy + hy + so)
        shadowPath.lineTo(center.x + sx + hx + wx + so, center.y + sy + hy + wy + so)
        shadowPath.lineTo(center.x + sx + wx + so, center.y + sy + wy + so)
        shadowPath.lineTo(center.x + sx + wx, center.y + sy + wy)
        shadowPath.close()
        canvas.drawPath(shadowPath, clockShadowPaint)
    }

    fun drawClock(canvas: Canvas) {

        val center = getClockCenter()
        val radius = getClockRadius()

        val batteryPaint = this.batteryPaint
        if (center.x != lastCenter.x || center.y != lastCenter.y || radius != lastRadius) {
            innerFaceRadius = radius.times(0.98f)
            outerFaceRadius = radius.times(1.02f)
            cdr = innerFaceRadius.times(10f / 108f)

            gradientMatrix.reset()
            gradientMatrix.preRotate(-90f, center.x, center.y)
            batteryPaint.shader = SweepGradient(center.x, center.y, Color.RED, Color.GREEN).apply {
                setLocalMatrix(gradientMatrix)
            }
            batteryPaint.strokeWidth = radius * 2f / 30f

            clipPath.reset()
            clipPath.addCircle(center.x, center.y, innerFaceRadius, Path.Direction.CW)

            innerClipRect.set(center.x - cdr, center.y - cdr, center.x + cdr, center.y + cdr)
            outerClipRect.set(
                center.x - innerFaceRadius, center.y - innerFaceRadius,
                center.x + innerFaceRadius, center.y + innerFaceRadius
            )
            val tangentHalf = Math.toDegrees(kotlin.math.asin((cdr / innerFaceRadius).toDouble())).toFloat()
            val angle1 = 45f - tangentHalf
            val angle2 = 45f + tangentHalf

            donutClip.reset()
            donutClip.arcTo(innerClipRect, -45f, 0f, true)
            donutClip.lineTo(
                center.x + innerFaceRadius * cos(angle1.toDouble() * PI / 180.0).toFloat(),
                center.y + innerFaceRadius * sin(angle1.toDouble() * PI / 180.0).toFloat()
            )
            donutClip.arcTo(outerClipRect, angle1, -(360f - (angle2 - angle1)))
            donutClip.lineTo(
                center.x + cdr * cos(135.0 * PI / 180.0).toFloat(),
                center.y + cdr * sin(135.0 * PI / 180.0).toFloat()
            )
            donutClip.arcTo(innerClipRect, 135f, -180f)
            donutClip.close()

            lastCenter.set(center.x, center.y)
            lastRadius = radius
        }

        canvas.drawArc(
            scratchRect.apply { set(center.x - radius, center.y - radius, center.x + radius, center.y + radius) },
            -90f,
            (batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) * 3.6f),
            false,
            batteryPaint
        )

        val bodyPaint = this.bodyPaint
        bodyPaint.color = backgroundImageUtils.getBaseColor()
        bodyPaint.style = Paint.Style.FILL

        // Main face
        canvas.drawCircle(center.x, center.y, innerFaceRadius, bodyPaint)

        bodyPaint.style = Paint.Style.STROKE
        bodyPaint.strokeWidth = radius / 40f
        // Border outline
        canvas.drawCircle(center.x, center.y, outerFaceRadius, bodyPaint)

        val now = frameCalendar
        now.timeInMillis = System.currentTimeMillis()
        val contrastColor = backgroundImageUtils.getColorOnBaseColor()

        if (!keyguardManager.isKeyguardLocked) {
            val currentTimeText =
                (if (now.get(Calendar.SECOND) % 2 == 0) evenSimpleDateFormat else oddSimpleDateFormat).format(
                    now
                )
            textPaint.color = backgroundImageUtils.getDarkerBaseColor()
            canvas.drawRect(
                scratchRect.apply { set(
                    center.x - (textBounds.width().toFloat() / 2f) - (radius/20f),
                    center.y - (radius / 2f) - (textBounds.height().toFloat()) + (radius/40f),
                    center.x + (textBounds.width().toFloat() / 2f) + (radius * 1.75f/40f),
                    center.y - (radius / 2f) + (radius * 4.75f/40f)
                ) }, textPaint
            )
            textPaint.color = backgroundImageUtils.getDarkBaseColor()
            canvas.drawRect(
                scratchRect.apply { set(
                    center.x - (textBounds.width().toFloat() / 2f) - (radius/40f),
                    center.y - (radius / 2f) - (textBounds.height().toFloat()) + (radius/20f),
                    center.x + (textBounds.width().toFloat() / 2f) + (radius/40f),
                    center.y - (radius / 2f) + (radius/10f)
                ) }, textPaint
            )
            textPaint.color = Color.argb(96, 128, 128, 128)
            canvas.drawText(
                allOnString,
                center.x - (textBounds.width().toFloat() / 2f),
                center.y - (radius / 2f) + (radius * 3f/40f),
                textPaint
            )
            textPaint.color = contrastColor
            canvas.drawText(
                currentTimeText,
                center.x - (textBounds.width().toFloat() / 2f),
                center.y - (radius / 2f) + (radius * 3f/40f),
                textPaint
            )
        }

        val clockPaint = this.clockPaint
        clockPaint.color = contrastColor
        clockPaint.style = Paint.Style.FILL

        // Compute geometry for all hands
        val hourGeom = getHandGeometry(now, radius / 18f, innerFaceRadius.times(0.75f), Calendar.HOUR)
        val minuteGeom = getHandGeometry(now, radius / 30f, innerFaceRadius, Calendar.MINUTE)
        val secondGeom = getHandGeometry(now, radius / 80f, innerFaceRadius, Calendar.SECOND)

        // Draw hand shadows clipped to donut (excludes center dot area)
        canvas.withClip(donutClip) {
            drawHandShadow(this, center, hourGeom.first, hourGeom.second, hourGeom.third, innerFaceRadius)
            drawHandShadow(this, center, minuteGeom.first, minuteGeom.second, minuteGeom.third, innerFaceRadius)
            drawHandShadow(this, center, secondGeom.first, secondGeom.second, secondGeom.third, innerFaceRadius)
        }

        // Center dot shadow drawn 3x, clipped to clock face only (not donut)
        canvas.withClip(clipPath) {
            val so = innerFaceRadius.times(2f)
            val csrD = cdr / sqrt(2f)
            scratchPath.reset()
            scratchPath.moveTo(center.x + csrD, center.y - csrD)
            scratchPath.rLineTo(so, so)
            scratchPath.rLineTo(-cdr * sqrt(2f), cdr * sqrt(2f))
            scratchPath.rLineTo(-so, -so)
            scratchPath.close()
            drawPath(scratchPath, clockShadowPaint)
            drawPath(scratchPath, clockShadowPaint)
            drawPath(scratchPath, clockShadowPaint)
        }

        // Draw hands clipped to clock face
        canvas.withClip(clipPath) {
            drawHandShape(this, center, hourGeom.first, hourGeom.second, hourGeom.third, clockPaint)
            drawHandShape(this, center, minuteGeom.first, minuteGeom.second, minuteGeom.third, clockPaint)
            drawHandShape(this, center, secondGeom.first, secondGeom.second, secondGeom.third, clockPaint)
        }

        // Center piece
        bodyPaint.style = Paint.Style.FILL
        canvas.drawCircle(center.x, center.y, radius / 12f, bodyPaint)
        clockPaint.style = Paint.Style.STROKE
        clockPaint.strokeWidth = radius / 54f
        canvas.drawCircle(center.x, center.y, radius / 12f, clockPaint)
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