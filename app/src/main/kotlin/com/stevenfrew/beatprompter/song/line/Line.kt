package com.stevenfrew.beatprompter.song.line

import android.graphics.Canvas
import android.graphics.Paint
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.graphics.LineGraphic
import com.stevenfrew.beatprompter.song.ScrollingMode

abstract class Line internal constructor(
	val lineTime: Long,
	val lineDuration: Long,
	val scrollMode: ScrollingMode,
	val songPixelPosition: Int,
	val isInChorusSection: Boolean,
	val yStartScrollTime: Long,
	val yStopScrollTime: Long,
	private val displaySettings: DisplaySettings
) {
	internal var previousLine: Line? = null
	internal var nextLine: Line? = null
	abstract val measurements: LineMeasurements
	protected val graphics =
		mutableListOf<LineGraphic>() // pointer to the allocated graphic, if one exists
	protected val canvasses = mutableListOf<Canvas>()

	internal abstract fun renderGraphics(paint: Paint)

	internal fun getTimeFromPixel(pixelPosition: Int): Long =
		if (pixelPosition == 0)
			0
		else if (pixelPosition >= songPixelPosition && pixelPosition < songPixelPosition + measurements.pixelsToTimes.size)
			measurements.pixelsToTimes[pixelPosition - songPixelPosition]
		else if (pixelPosition < songPixelPosition && previousLine != null)
			previousLine!!.getTimeFromPixel(pixelPosition)
		else if (pixelPosition >= songPixelPosition + measurements.pixelsToTimes.size && nextLine != null)
			nextLine!!.getTimeFromPixel(pixelPosition)
		else
			measurements.pixelsToTimes[measurements.pixelsToTimes.size - 1]

	internal fun getPixelFromTime(time: Long): Int =
		if (time == 0L)
			0
		// line end trim
		else (if (nextLine == null) Long.MAX_VALUE else nextLine!!.lineTime).let {
			if (time in lineTime until it)
				calculatePixelFromTime(time)
			if (time < lineTime && previousLine != null)
				previousLine!!.getPixelFromTime(time)
			if (time >= it && nextLine != null)
				nextLine!!.getPixelFromTime(time)
			songPixelPosition + measurements.pixelsToTimes.size
		}

	private fun calculatePixelFromTime(time: Long): Int =
		songPixelPosition + measurements.findClosestEarliestPixel(time)

	internal fun allocateGraphic(graphic: LineGraphic) {
		graphics.add(graphic)
		canvasses.add(Canvas(graphic.bitmap))
	}

	internal fun getGraphics(paint: Paint): List<LineGraphic> {
		renderGraphics(paint)
		return graphics
	}

	internal open fun recycleGraphics() {
		graphics.forEach { it.recycle() }
		canvasses.clear()
	}

	internal fun isFullyOnScreen(currentSongPixelPosition: Int): Boolean =
		currentSongPixelPosition in (songPixelPosition + measurements.lineHeight) - displaySettings.usableScreenHeight..songPixelPosition

	internal fun screenCoverage(currentSongPixelPosition: Int): Double =
		// If line is off top of screen, no coverage
		if (currentSongPixelPosition > songPixelPosition + measurements.lineHeight)
			0.0
		// If line is off end of screen, no coverage
		else if (currentSongPixelPosition < songPixelPosition - displaySettings.usableScreenHeight)
			0.0
		// If line fills or covers screen, full coverage!
		else if (currentSongPixelPosition >= songPixelPosition && currentSongPixelPosition <= songPixelPosition + measurements.lineHeight)
			1.0
		// line amount before point
		else (currentSongPixelPosition - songPixelPosition).let {
			// If line crosses top boundary, return remainder
			if (it > measurements.lineHeight)
				(measurements.lineHeight - it) / displaySettings.usableScreenHeight.toDouble()
			// If line crosses bottom boundary, return remainder
			val lineAmountBeforeScreenEnd =
				(currentSongPixelPosition + displaySettings.usableScreenHeight) - songPixelPosition
			if (lineAmountBeforeScreenEnd in 0..measurements.lineHeight)
				lineAmountBeforeScreenEnd / displaySettings.usableScreenHeight.toDouble()
			// Only other scenario is: line entirely onscreen
			measurements.lineHeight / displaySettings.usableScreenHeight.toDouble()
		}
}
