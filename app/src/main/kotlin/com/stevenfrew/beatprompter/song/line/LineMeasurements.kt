package com.stevenfrew.beatprompter.song.line

import android.graphics.Rect
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.util.Utils
import kotlin.math.max
import kotlin.math.min

class LineMeasurements internal constructor(
	internal val lines: Int,
	internal val lineWidth: Int,
	internal val lineHeight: Int,
	internal val graphicHeights: IntArray,
	lineTime: Long,
	lineDuration: Long,
	yStartScrollTime: Long,
	scrollMode: ScrollingMode
) {
	internal val pixelsToTimes: LongArray
	internal val jumpScrollIntervals = IntArray(101)
	internal val graphicRectangles =
		graphicHeights.map { Rect(0, 0, lineWidth, it) }.toTypedArray()

	init {
		repeat(101) {
			jumpScrollIntervals[it] = min(
				(lineHeight.toDouble() * Utils.mSineLookup[(90.0 * (it.toDouble() / 100.0)).toInt()]).toInt(),
				lineHeight
			)
		}

		pixelsToTimes = LongArray(max(1, lineHeight))
		val lineEndTime = lineTime + lineDuration
		val timeDiff = lineEndTime - yStartScrollTime
		pixelsToTimes[0] = lineTime
		for (f in 1 until lineHeight) {
			val linePercentage = f.toDouble() / lineHeight.toDouble()
			val diff =
				if (scrollMode == ScrollingMode.Beat)
					(timeDiff * Utils.mSineLookup[(90.0 * linePercentage).toInt()]).toLong()
				else
					(linePercentage * timeDiff.toDouble()).toLong()
			pixelsToTimes[f] = yStartScrollTime + diff
		}
	}

	fun findClosestEarliestPixel(time: Long): Int =
		findClosestEarliestPixel(time, 0, pixelsToTimes.size - 1, 0)

	private fun findClosestEarliestPixel(time: Long, left: Int, right: Int, bestIndex: Int): Int =
		if (left > right)
			bestIndex
		else {
			val currentBestVal = pixelsToTimes[bestIndex]
			val mid = (left + right) / 2
			val midVal = pixelsToTimes[mid]
			if (midVal > time || time - midVal > time - currentBestVal)
				findClosestEarliestPixel(time, left, mid - 1, bestIndex)
			else //if(midVal<time && time-midVal<time-currentBestVal)
				findClosestEarliestPixel(time, mid + 1, right, mid)
		}
}
