package com.stevenfrew.beatprompter.song.line

import android.graphics.Rect
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.util.Utils
import kotlin.math.max
import kotlin.math.min

class LineMeasurements internal constructor(
	internal val mLines: Int,
	internal val mLineWidth: Int,
	internal val mLineHeight: Int,
	internal val mGraphicHeights: IntArray,
	lineTime: Long,
	lineDuration: Long,
	yStartScrollTime: Long,
	scrollMode: ScrollingMode
) {
	internal val mPixelsToTimes: LongArray
	internal val mJumpScrollIntervals = IntArray(101)
	internal val mGraphicRectangles =
		mGraphicHeights.map { Rect(0, 0, mLineWidth, it) }.toTypedArray()

	init {
		repeat(101) {
			mJumpScrollIntervals[it] = min(
				(mLineHeight.toDouble() * Utils.mSineLookup[(90.0 * (it.toDouble() / 100.0)).toInt()]).toInt(),
				mLineHeight
			)
		}

		mPixelsToTimes = LongArray(max(1, mLineHeight))
		val lineEndTime = lineTime + lineDuration
		val timeDiff = lineEndTime - yStartScrollTime
		mPixelsToTimes[0] = lineTime
		for (f in 1 until mLineHeight) {
			val linePercentage = f.toDouble() / mLineHeight.toDouble()
			val diff =
				if (scrollMode == ScrollingMode.Beat)
					(timeDiff * Utils.mSineLookup[(90.0 * linePercentage).toInt()]).toLong()
				else
					(linePercentage * timeDiff.toDouble()).toLong()
			mPixelsToTimes[f] = yStartScrollTime + diff
		}
	}

	fun findClosestEarliestPixel(time: Long): Int {
		return findClosestEarliestPixel(time, 0, mPixelsToTimes.size - 1, 0)
	}

	private fun findClosestEarliestPixel(time: Long, left: Int, right: Int, bestIndex: Int): Int {
		if (left > right)
			return bestIndex
		val currentBestVal = mPixelsToTimes[bestIndex]
		val mid = (left + right) / 2
		val midVal = mPixelsToTimes[mid]
		return if (midVal > time || time - midVal > time - currentBestVal)
			findClosestEarliestPixel(time, left, mid - 1, bestIndex)
		else //if(midVal<time && time-midVal<time-currentBestVal)
			findClosestEarliestPixel(time, mid + 1, right, mid)
	}
}
