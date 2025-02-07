package com.riskycase.jarvisEnhanced.util

import android.app.KeyguardManager
import android.graphics.PointF
import android.icu.util.Calendar
import javax.inject.Inject

class ClockConstants @Inject constructor(val keyguardManager: KeyguardManager) {

    private var width = 0
    private var height = 0
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

    private fun getHandRotations(calendar: Calendar, value: Int): Float {
        return when (value) {
            Calendar.HOUR -> ((calendar.get(Calendar.HOUR)
                .toFloat() / 12f) + (calendar.get(Calendar.MINUTE).toFloat() / 720f) + (calendar.get(
                Calendar.SECOND
            ).toFloat() / 43200f) + calendar.get(Calendar.MILLISECOND).toFloat() / 43200000f) * 360f

            Calendar.MINUTE -> ((calendar.get(Calendar.MINUTE)
                .toFloat() / 60f) + (calendar.get(Calendar.SECOND).toFloat() / 3600f) + calendar.get(
                Calendar.MILLISECOND
            ).toFloat() / 3600000f) * 360f

            Calendar.SECOND -> ((calendar.get(Calendar.SECOND)
                .toFloat() / 60f) + calendar.get(Calendar.MILLISECOND).toFloat() / 60000f) * 360f

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
                    hourHandRotationAnimator.setAnimation(
                        System.currentTimeMillis() + lockUnlockAnimationDuration,
                        clockSweepAnimationDuration,
                        0f,
                        getHandRotations(finalCalendar, Calendar.HOUR)
                    )
                    minuteHandRotationAnimator.setAnimation(
                        System.currentTimeMillis() + lockUnlockAnimationDuration,
                        clockSweepAnimationDuration,
                        0f,
                        getHandRotations(finalCalendar, Calendar.MINUTE)
                    )
                    secondHandRotationAnimator.setAnimation(
                        System.currentTimeMillis() + lockUnlockAnimationDuration,
                        clockSweepAnimationDuration,
                        0f,
                        getHandRotations(finalCalendar, Calendar.SECOND)
                    )
                }
            }
            previousVisibleState = currentVisibleState
        }
    }

    fun getClockCenter(): PointF {
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

    fun getClockRadius(): Float {
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

    fun getRotation(value: Int): Float {
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

}