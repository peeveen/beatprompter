package com.stevenfrew.beatprompter.song.line

import android.graphics.Canvas
import android.graphics.Paint
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.graphics.LineGraphic
import com.stevenfrew.beatprompter.song.ScrollingMode

abstract class Line internal constructor(
	val mLineTime: Long,
	val mLineDuration: Long,
	val mScrollMode: ScrollingMode,
	val mSongPixelPosition: Int,
	val mInChorusSection: Boolean,
	val mYStartScrollTime: Long,
	val mYStopScrollTime: Long,
	private val mDisplaySettings: DisplaySettings
) {
	internal var mPrevLine: Line? = null
	internal var mNextLine: Line? = null
	abstract val mMeasurements: LineMeasurements
	protected val mGraphics =
		mutableListOf<LineGraphic>() // pointer to the allocated graphic, if one exists
	protected val mCanvasses = mutableListOf<Canvas>()

	internal abstract fun renderGraphics(paint: Paint)

	internal fun getTimeFromPixel(pixelPosition: Int): Long =
		if (pixelPosition == 0)
			0
		else if (pixelPosition >= mSongPixelPosition && pixelPosition < mSongPixelPosition + mMeasurements.mPixelsToTimes.size)
			mMeasurements.mPixelsToTimes[pixelPosition - mSongPixelPosition]
		else if (pixelPosition < mSongPixelPosition && mPrevLine != null)
			mPrevLine!!.getTimeFromPixel(pixelPosition)
		else if (pixelPosition >= mSongPixelPosition + mMeasurements.mPixelsToTimes.size && mNextLine != null)
			mNextLine!!.getTimeFromPixel(pixelPosition)
		else
			mMeasurements.mPixelsToTimes[mMeasurements.mPixelsToTimes.size - 1]

	internal fun getPixelFromTime(time: Long): Int =
		if (time == 0L)
			0
		// line end trim
		else (if (mNextLine == null) Long.MAX_VALUE else mNextLine!!.mLineTime).let {
			if (time in mLineTime until it)
				calculatePixelFromTime(time)
			if (time < mLineTime && mPrevLine != null)
				mPrevLine!!.getPixelFromTime(time)
			if (time >= it && mNextLine != null)
				mNextLine!!.getPixelFromTime(time)
			mSongPixelPosition + mMeasurements.mPixelsToTimes.size
		}

	private fun calculatePixelFromTime(time: Long): Int =
		mSongPixelPosition + mMeasurements.findClosestEarliestPixel(time)

	internal fun allocateGraphic(graphic: LineGraphic) {
		mGraphics.add(graphic)
		mCanvasses.add(Canvas(graphic.bitmap))
	}

	internal fun getGraphics(paint: Paint): List<LineGraphic> {
		renderGraphics(paint)
		return mGraphics
	}

	internal open fun recycleGraphics() {
		mGraphics.forEach { it.recycle() }
		mCanvasses.clear()
	}

	internal fun isFullyOnScreen(currentSongPixelPosition: Int): Boolean =
		currentSongPixelPosition in (mSongPixelPosition + mMeasurements.mLineHeight) - mDisplaySettings.mUsableScreenHeight..mSongPixelPosition

	internal fun screenCoverage(currentSongPixelPosition: Int): Double =
		// If line is off top of screen, no coverage
		if (currentSongPixelPosition > mSongPixelPosition + mMeasurements.mLineHeight)
			0.0
		// If line is off end of screen, no coverage
		else if (currentSongPixelPosition < mSongPixelPosition - mDisplaySettings.mUsableScreenHeight)
			0.0
		// If line fills or covers screen, full coverage!
		else if (currentSongPixelPosition >= mSongPixelPosition && currentSongPixelPosition <= mSongPixelPosition + mMeasurements.mLineHeight)
			1.0
		// line amount before point
		else (currentSongPixelPosition - mSongPixelPosition).let {
			// If line crosses top boundary, return remainder
			if (it > mMeasurements.mLineHeight)
				(mMeasurements.mLineHeight - it) / mDisplaySettings.mUsableScreenHeight.toDouble()
			// If line crosses bottom boundary, return remainder
			val lineAmountBeforeScreenEnd =
				(currentSongPixelPosition + mDisplaySettings.mUsableScreenHeight) - mSongPixelPosition
			if (lineAmountBeforeScreenEnd in 0..mMeasurements.mLineHeight)
				lineAmountBeforeScreenEnd / mDisplaySettings.mUsableScreenHeight.toDouble()
			// Only other scenario is: line entirely onscreen
			mMeasurements.mLineHeight / mDisplaySettings.mUsableScreenHeight.toDouble()
		}
}
