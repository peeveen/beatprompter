package com.stevenfrew.beatprompter

import android.graphics.Paint
import android.graphics.Typeface
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.songload.CancelEvent
import com.stevenfrew.beatprompter.event.ColorEvent
import com.stevenfrew.beatprompter.event.LineEvent
import java.util.ArrayList

abstract class Line internal constructor(lineTime: Long,lineDuration:Long,internal var mColorEvent: ColorEvent // style event that occurred immediately before this line will be shown.
                                         ,val mBeatInfo:LineBeatInfo) {
    internal var mPrevLine: Line? = null
    internal var mNextLine: Line? = null
    internal var mSongPixelPosition: Int = 0
    var mLineEvent: LineEvent=LineEvent(lineTime,this,lineDuration) // the LineEvent that will display this line.
    internal var mGraphics = ArrayList<LineGraphic>() // pointer to the allocated graphic, if one exists
    internal var mLineMeasurements: LineMeasurements? = null
    var mYStartScrollTime: Long = 0
    var mYStopScrollTime: Long = 0

    val lastLine: Line
        get() {
            var l: Line = this
            while (true) {
                if (l.mNextLine == null)
                    return l
                l = l.mNextLine!!
            }
        }

    internal val graphics: Collection<LineGraphic>
        get() = getGraphics(true)


    internal fun measure(paint: Paint, minimumFontSize: Float, maximumFontSize: Float, screenWidth: Int, screenHeight: Int, font: Typeface, highlightColour: Int, defaultHighlightColour: Int, errors: MutableList<FileParseError>, songPixelPosition: Int, scrollMode: ScrollingMode, cancelEvent: CancelEvent): Int {
        mSongPixelPosition = songPixelPosition
        mLineMeasurements = doMeasurements(paint, minimumFontSize, maximumFontSize, screenWidth, screenHeight, font, highlightColour, defaultHighlightColour, errors, scrollMode, cancelEvent)
        return if (mLineMeasurements != null) mLineMeasurements!!.mHighlightColour else 0
    }

    abstract fun doMeasurements(paint: Paint, minimumFontSize: Float, maximumFontSize: Float, screenWidth: Int, screenHeight: Int, font: Typeface, highlightColour: Int, defaultHighlightColour: Int, errors: MutableList<FileParseError>, scrollMode: ScrollingMode, cancelEvent: CancelEvent): LineMeasurements?

    internal fun getTimeFromPixel(pixelPosition: Int): Long {
        if (pixelPosition == 0)
            return 0
        if (pixelPosition >= mSongPixelPosition && pixelPosition < mSongPixelPosition + mLineMeasurements!!.mPixelsToTimes.size)
            return mLineMeasurements!!.mPixelsToTimes[pixelPosition - mSongPixelPosition]
        else if (pixelPosition < mSongPixelPosition && mPrevLine != null)
            return mPrevLine!!.getTimeFromPixel(pixelPosition)
        else if (pixelPosition >= mSongPixelPosition + mLineMeasurements!!.mPixelsToTimes.size && mNextLine != null)
            return mNextLine!!.getTimeFromPixel(pixelPosition)
        return mLineMeasurements!!.mPixelsToTimes[mLineMeasurements!!.mPixelsToTimes.size - 1]
    }

    internal fun getPixelFromTime(time: Long): Int {
        if (time == 0L)
            return 0
        var lineEndTime = Long.MAX_VALUE
        if (mNextLine != null)
            lineEndTime = mNextLine!!.mLineEvent.mEventTime

        if (time >= mLineEvent.mEventTime && time < lineEndTime)
            return calculatePixelFromTime(time)
        else if (time < mLineEvent.mEventTime && mPrevLine != null)
            return mPrevLine!!.getPixelFromTime(time)
        else if (time >= lineEndTime && mNextLine != null)
            return mNextLine!!.getPixelFromTime(time)
        return mSongPixelPosition + mLineMeasurements!!.mPixelsToTimes.size
    }

    private fun calculatePixelFromTime(time: Long): Int {
        var last = mSongPixelPosition
        for (n in mLineMeasurements!!.mPixelsToTimes) {
            if (n > time)
                return last
            last++
        }
        return last
    }

    internal fun setGraphic(graphic: LineGraphic) {
        mGraphics.add(graphic)
    }

    internal abstract fun getGraphics(allocate: Boolean): Collection<LineGraphic>

    fun insertAfter(line: Line) {
        line.mNextLine = mNextLine
        if (mNextLine != null)
            mNextLine!!.mPrevLine = line
        line.mPrevLine = this
        this.mNextLine = line
    }

    internal open fun recycleGraphics() {
        getGraphics(false).forEach{it.recycle()}
    }
}
