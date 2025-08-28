package com.riskycase.jarvisEnhanced.util.wallpaper

import android.graphics.PointF
import androidx.compose.animation.core.EaseOut

abstract class Animator<T : Comparable<T>, V : Any>(initialT: T, initialV: V) {

    protected var previousValue: T = initialT
    protected val bezierEasing = EaseOut

    protected var startTime: Long = Long.MAX_VALUE
    protected var endTime: Long = Long.MAX_VALUE

    protected var startValue: V = initialV
    protected var endValue: V = initialV

    fun setAnimation(startTime: Long, duration: Long, startValue: V, endValue: V) {
        this.startTime = startTime
        this.endTime = startTime + duration
        this.startValue = startValue
        this.endValue = endValue
    }

    fun forceValue(value: V) {
        this.startValue = value
        this.endValue = value
    }

    protected fun scaleValue(
        startValue: Float,
        endValue: Float,
        startTime: Long,
        currentTime: Long,
        endTime: Long
    ): Float {
        return startValue + ((endValue - startValue) * bezierEasing.transform((currentTime - startTime).toFloat() / (endTime - startTime).toFloat()))
    }

    abstract fun getValue(time: Long): V

    abstract fun getValue(time: Long, fallbackV: V): V
}


class AnimatorFloat<T : Comparable<T>>(initialT: T, initialFloat: Float) :
    Animator<T, Float>(initialT, initialFloat) {
    override fun getValue(time: Long): Float {
        return getValue(time, endValue)
    }

    override fun getValue(time: Long, fallbackV: Float): Float {
        return if (time < this.startTime) startValue
        else if (time > this.endTime) fallbackV
        else scaleValue(startValue, endValue, startTime, time, endTime)
    }
}

class AnimatorPointF<T : Comparable<T>>(initialT: T, initialPointF: PointF) :
    Animator<T, PointF>(initialT, initialPointF) {
    override fun getValue(time: Long): PointF {
        return getValue(time, endValue)
    }

    override fun getValue(time: Long, fallbackV: PointF): PointF {
        return if (time < this.startTime) startValue
        else if (time > this.endTime) fallbackV
        else PointF(
            scaleValue(startValue.x, endValue.x, startTime, time, endTime),
            scaleValue(startValue.y, endValue.y, startTime, time, endTime),
        )
    }
}