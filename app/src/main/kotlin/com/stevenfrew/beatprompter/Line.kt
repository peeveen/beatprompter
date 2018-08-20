package com.stevenfrew.beatprompter

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.songload.CancelEvent
import com.stevenfrew.beatprompter.event.LineEvent
import com.stevenfrew.beatprompter.songload.SongLoadCancelledException
import java.util.ArrayList

abstract class Line internal constructor(val mLineTime:Long,val mLineDuration:Long,val mScrollMode:ScrollingMode,val mDisplaySettings:SongDisplaySettings,val mSongPixelPosition:Int) {
    internal var mPrevLine: Line? = null
    internal var mNextLine: Line? = null
    abstract val mMeasurements:LineMeasurements
    internal var mGraphics = mutableListOf<LineGraphic>() // pointer to the allocated graphic, if one exists
    var mYStartScrollTime: Long = 0
    var mYStopScrollTime: Long = 0

    internal abstract fun renderGraphics(allocate: Boolean)

    internal fun getTimeFromPixel(pixelPosition: Int): Long {
        if (pixelPosition == 0)
            return 0
        if (pixelPosition >= mSongPixelPosition && pixelPosition < mSongPixelPosition + mMeasurements.mPixelsToTimes.size)
            return mMeasurements.mPixelsToTimes[pixelPosition - mSongPixelPosition]
        else if (pixelPosition < mSongPixelPosition && mPrevLine != null)
            return mPrevLine!!.getTimeFromPixel(pixelPosition)
        else if (pixelPosition >= mSongPixelPosition + mMeasurements.mPixelsToTimes.size && mNextLine != null)
            return mNextLine!!.getTimeFromPixel(pixelPosition)
        return mMeasurements.mPixelsToTimes[mMeasurements.mPixelsToTimes.size - 1]
    }

    internal fun getPixelFromTime(time: Long): Int {
        if (time == 0L)
            return 0
        var lineEndTime = Long.MAX_VALUE
        if (mNextLine != null)
            lineEndTime = mNextLine!!.mLineTime

        if (time >= mLineTime && time < lineEndTime)
            return calculatePixelFromTime(time)
        else if (time < mLineTime && mPrevLine != null)
            return mPrevLine!!.getPixelFromTime(time)
        else if (time >= lineEndTime && mNextLine != null)
            return mNextLine!!.getPixelFromTime(time)
        return mSongPixelPosition + mMeasurements.mPixelsToTimes.size
    }

    private fun calculatePixelFromTime(time: Long): Int {
        var last = mSongPixelPosition
        for (n in mMeasurements.mPixelsToTimes) {
            if (n > time)
                return last
            last++
        }
        return last
    }

    internal fun renderGraphics() {
        renderGraphics(true)
    }

    internal fun setGraphic(graphic: LineGraphic) {
        mGraphics.add(graphic)
    }

    internal open fun recycleGraphics() {
        mGraphics.forEach{it.recycle()}
    }
}
