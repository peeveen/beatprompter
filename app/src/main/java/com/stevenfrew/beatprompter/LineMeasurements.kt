package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.event.LineEvent
import java.util.ArrayList

class LineMeasurements internal constructor(@JvmField internal var mLines: Int, @JvmField internal var mLineWidth: Int, @JvmField internal var mLineHeight: Int, graphicHeights: ArrayList<Int>, @JvmField internal var mHighlightColour: Int, lineEvent: LineEvent, nextLine: Line?, yStartScrollTime: Long, scrollMode: ScrollingMode) {
    @JvmField internal var mPixelsToTimes: LongArray
    @JvmField internal var mGraphicHeights: IntArray
    @JvmField internal var mJumpScrollIntervals = IntArray(101)

    init {
        mGraphicHeights = IntArray(graphicHeights.size)
        for (f in mGraphicHeights.indices)
            mGraphicHeights[f] = graphicHeights[f]

        for (f in 0..100) {
            val percentage = f.toDouble() / 100.0
            mJumpScrollIntervals[f] = Math.min((mLineHeight.toDouble() * Utils.mSineLookup[(90.0 * percentage).toInt()]).toInt(), mLineHeight)
        }

        mPixelsToTimes = LongArray(Math.max(1, mLineHeight))
        val lineStartTime = lineEvent.mEventTime
        val lineEndTime = lineEvent.mEventTime + if (scrollMode == ScrollingMode.Smooth || nextLine != null) lineEvent.mDuration else 0
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
