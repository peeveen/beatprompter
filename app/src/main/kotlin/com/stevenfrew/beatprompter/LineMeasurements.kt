package com.stevenfrew.beatprompter

import android.graphics.Rect

class LineMeasurements internal constructor(internal var mLines: Int, internal var mLineWidth: Int, internal var mLineHeight: Int, internal val mGraphicHeights: IntArray, internal var mHighlightColor: Int?, lineTime:Long,lineDuration: Long, nextLine: Line?, yStartScrollTime: Long, scrollMode: ScrollingMode, val mScreenSize: Rect) {
    internal var mPixelsToTimes: LongArray
    internal var mJumpScrollIntervals = IntArray(101)

    init {
        for (f in 0..100)
            mJumpScrollIntervals[f] = Math.min((mLineHeight.toDouble() * Utils.mSineLookup[(90.0 * (f.toDouble() / 100.0)).toInt()]).toInt(), mLineHeight)

        mPixelsToTimes = LongArray(Math.max(1, mLineHeight))
        val lineStartTime = lineTime
        val lineEndTime = lineStartTime + if (scrollMode == ScrollingMode.Smooth || nextLine != null) lineDuration else 0
        val timeDiff = lineEndTime - yStartScrollTime
        mPixelsToTimes[0] = lineStartTime
        for (f in 1 until mLineHeight) {
            val linePercentage = f.toDouble() / mLineHeight.toDouble()
            if (scrollMode == ScrollingMode.Beat) {
                val sineLookup = (90.0 * linePercentage).toInt()
                val sineTimeDiff = (timeDiff * Utils.mSineLookup[sineLookup]).toLong()
                mPixelsToTimes[f] = yStartScrollTime + sineTimeDiff
            } else {
                val pixelTimeDiff = (linePercentage * timeDiff.toDouble()).toLong()
                mPixelsToTimes[f] = yStartScrollTime + pixelTimeDiff
            }
        }
    }
}
