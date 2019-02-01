package com.stevenfrew.beatprompter.song.line

import android.graphics.Paint
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.graphics.LineGraphic
import com.stevenfrew.beatprompter.song.ScrollingMode

abstract class Line internal constructor(val mLineTime: Long,
                                         val mLineDuration: Long,
                                         val mScrollMode: ScrollingMode,
                                         val mSongPixelPosition: Int,
                                         val mInChorusSection: Boolean,
                                         val mYStartScrollTime: Long,
                                         val mYStopScrollTime: Long,
                                         private val mDisplaySettings: DisplaySettings) {
    internal var mPrevLine: Line? = null
    internal var mNextLine: Line? = null
    abstract val mMeasurements: LineMeasurements
    protected var mGraphics = mutableListOf<LineGraphic>() // pointer to the allocated graphic, if one exists

    internal abstract fun renderGraphics(paint: Paint)

    internal fun getTimeFromPixel(pixelPosition: Int): Long {
        if (pixelPosition == 0)
            return 0
        if (pixelPosition >= mSongPixelPosition && pixelPosition < mSongPixelPosition + mMeasurements.mPixelsToTimes.size)
            return mMeasurements.mPixelsToTimes[pixelPosition - mSongPixelPosition]
        if (pixelPosition < mSongPixelPosition && mPrevLine != null)
            return mPrevLine!!.getTimeFromPixel(pixelPosition)
        if (pixelPosition >= mSongPixelPosition + mMeasurements.mPixelsToTimes.size && mNextLine != null)
            return mNextLine!!.getTimeFromPixel(pixelPosition)
        return mMeasurements.mPixelsToTimes[mMeasurements.mPixelsToTimes.size - 1]
    }

    internal fun getPixelFromTime(time: Long): Int {
        if (time == 0L)
            return 0
        val lineEndTime = if (mNextLine == null) Long.MAX_VALUE else mNextLine!!.mLineTime
        if (time in mLineTime..(lineEndTime - 1))
            return calculatePixelFromTime(time)
        if (time < mLineTime && mPrevLine != null)
            return mPrevLine!!.getPixelFromTime(time)
        if (time >= lineEndTime && mNextLine != null)
            return mNextLine!!.getPixelFromTime(time)
        return mSongPixelPosition + mMeasurements.mPixelsToTimes.size
    }

    private fun calculatePixelFromTime(time: Long): Int {
        return mSongPixelPosition + mMeasurements.findClosestEarliestPixel(time)
    }

    internal fun allocateGraphic(graphic: LineGraphic) {
        mGraphics.add(graphic)
    }

    internal fun getGraphics(paint: Paint): List<LineGraphic> {
        renderGraphics(paint)
        return mGraphics
    }

    internal open fun recycleGraphics() {
        mGraphics.forEach { it.recycle() }
    }

    internal fun isFullyOnScreen(currentSongPixelPosition: Int): Boolean {
        return currentSongPixelPosition in (mSongPixelPosition + mMeasurements.mLineHeight) - mDisplaySettings.mUsableScreenHeight..mSongPixelPosition
    }

    internal fun screenCoverage(currentSongPixelPosition: Int): Double {
        // If line is off top of screen, no coverage
        if (currentSongPixelPosition > mSongPixelPosition + mMeasurements.mLineHeight)
            return 0.0
        // If line is off end of screen, no coverage
        if (currentSongPixelPosition < mSongPixelPosition - mDisplaySettings.mUsableScreenHeight)
            return 0.0
        // If line fills or covers screen, full coverage!
        if (currentSongPixelPosition >= mSongPixelPosition && currentSongPixelPosition <= mSongPixelPosition + mMeasurements.mLineHeight)
            return 1.0
        // If line crosses top boundary, return remainder
        val lineAmountBeforePoint = currentSongPixelPosition - mSongPixelPosition
        if (lineAmountBeforePoint > mMeasurements.mLineHeight)
            return (mMeasurements.mLineHeight - lineAmountBeforePoint) / mDisplaySettings.mUsableScreenHeight.toDouble()
        // If line crosses bottom boundary, return remainder
        val lineAmountBeforeScreenEnd = (currentSongPixelPosition + mDisplaySettings.mUsableScreenHeight) - mSongPixelPosition
        if (lineAmountBeforeScreenEnd in 0..mMeasurements.mLineHeight)
            return lineAmountBeforeScreenEnd / mDisplaySettings.mUsableScreenHeight.toDouble()
        // Only other scenario is: line entirely onscreen
        return mMeasurements.mLineHeight / mDisplaySettings.mUsableScreenHeight.toDouble()
    }
}
