package com.stevenfrew.beatprompter

class LineMeasurements internal constructor(internal var mLines: Int, internal var mLineWidth: Int, internal var mLineHeight: Int, internal val mGraphicHeights: IntArray, lineTime:Long, lineDuration: Long, yStartScrollTime: Long, lineScrollMode: LineScrollingMode) {
    internal var mPixelsToTimes: LongArray
    internal var mJumpScrollIntervals = IntArray(101)

    init {
        for (f in 0..100)
            mJumpScrollIntervals[f] = Math.min((mLineHeight.toDouble() * Utils.mSineLookup[(90.0 * (f.toDouble() / 100.0)).toInt()]).toInt(), mLineHeight)

        mPixelsToTimes = LongArray(Math.max(1, mLineHeight))
        // TODO: worry about smooth duration?
        val lineEndTime = lineTime + lineDuration
        val timeDiff = lineEndTime - yStartScrollTime
        mPixelsToTimes[0] = lineTime
        for (f in 1 until mLineHeight) {
            val linePercentage = f.toDouble() / mLineHeight.toDouble()
            val diff=
                if (lineScrollMode == LineScrollingMode.Beat)
                    (timeDiff * Utils.mSineLookup[(90.0 * linePercentage).toInt()]).toLong()
                else
                    (linePercentage * timeDiff.toDouble()).toLong()
            mPixelsToTimes[f] = yStartScrollTime + diff
        }
    }
}
