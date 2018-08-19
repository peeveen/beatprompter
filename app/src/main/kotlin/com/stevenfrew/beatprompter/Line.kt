package com.stevenfrew.beatprompter

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.songload.CancelEvent
import com.stevenfrew.beatprompter.event.LineEvent
import com.stevenfrew.beatprompter.songload.SongLoadCancelledException
import java.util.ArrayList

abstract class Line internal constructor(lineTime: Long,lineDuration:Long,val mBeatInfo:BeatInfo) {
    internal var mPrevLine: Line? = null
    internal var mNextLine: Line? = null
    internal var mSongPixelPosition: Int = 0
    var mLineEvent: LineEvent=LineEvent(lineTime,this,lineDuration) // the LineEvent that will display this line.
    internal var mGraphics = ArrayList<LineGraphic>() // pointer to the allocated graphic, if one exists
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


    internal fun measure(paint: Paint, songDisplaySettings: SongDisplaySettings, font: Typeface, highlightColour: Int, defaultHighlightColour: Int, errors: MutableList<FileParseError>, songPixelPosition: Int, scrollMode: ScrollingMode, cancelEvent: CancelEvent): Int {
        mSongPixelPosition = songPixelPosition
        try {
            return doMeasurements(paint, songDisplaySettings, font, highlightColour, defaultHighlightColour, errors, scrollMode, cancelEvent).mHighlightColour
        }
        catch(ex: SongLoadCancelledException)
        {
            return 0
        }
    }

    abstract fun doMeasurements(paint: Paint, songDisplaySettings: SongDisplaySettings, font: Typeface, highlightColour: Int, defaultHighlightColour: Int, errors: MutableList<FileParseError>, scrollMode: ScrollingMode, cancelEvent: CancelEvent): LineMeasurements

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
