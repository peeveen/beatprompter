package com.stevenfrew.beatprompter

import android.graphics.*
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.event.BaseEvent
import com.stevenfrew.beatprompter.midi.BeatBlock
import com.stevenfrew.beatprompter.midi.OutgoingMessage

class Song(val mSongFile:SongFile,val mScrollMode:ScrollingMode, val mDisplaySettings:SongDisplaySettings, val mSongHeight:Int,
           private val mEvents:List<BaseEvent>, private val mLines:List<Line>,
           val mInitialMIDIMessages:List<OutgoingMessage>, private val mBeatBlocks:List<BeatBlock>, val mSendMIDIClock:Boolean,
           val mStartScreenStrings:List<ScreenString>, val mTotalStartScreenTextHeight:Int,
           val mStartedByBandLeader:Boolean, val mNextSong:String, val mNextSongString:ScreenString?,
           val mSmoothScrollOffset:Int, val mBeatCounterRect:Rect, val mSongTitleHeader:ScreenString,val mSongTitleHeaderLocation:PointF) {
    internal var mCurrentLine: Line? = mLines.firstOrNull()
    internal var mLastLine: Line? = mLines.lastOrNull()
    private var mFirstEvent: BaseEvent=mEvents.first() // First event in the event chain.
    internal var mCurrentEvent: BaseEvent? = mEvents.firstOrNull() // Last event that executed.
    private var mNextEvent: BaseEvent? = mCurrentEvent?.mNextEvent // Upcoming event.
    var mCancelled = false
    private val mNumberOfMIDIBeatBlocks = mBeatBlocks.size

    internal fun setProgress(nano: Long) {
        var e = mCurrentEvent
        if (e == null)
            e = mFirstEvent
        val newCurrentEvent = e.findEventOnOrBefore(nano)
        mCurrentEvent = newCurrentEvent
        mNextEvent = mCurrentEvent!!.mNextEvent
        val newCurrentLineEvent = newCurrentEvent!!.mPrevLineEvent
        mCurrentLine = newCurrentLineEvent?.mLine ?: mLines.firstOrNull()
    }

    internal fun getNextEvent(time: Long): BaseEvent? {
        if (mNextEvent != null && mNextEvent!!.mEventTime <= time) {
            mCurrentEvent = mNextEvent
            mNextEvent = mNextEvent!!.mNextEvent
            return mCurrentEvent
        }
        return null
    }

    internal fun getTimeFromPixel(pixel: Int): Long {
        if (pixel == 0)
            return 0
        return if (mCurrentLine != null)
                mCurrentLine!!.getTimeFromPixel(pixel)
            else
                mLines.firstOrNull()?.getTimeFromPixel(pixel)?:0
    }

    internal fun getPixelFromTime(time: Long): Int {
        if (time == 0L)
            return 0
        return if (mCurrentLine != null) mCurrentLine!!.getPixelFromTime(time) else mLines.firstOrNull()?.getPixelFromTime(time)?:0
    }

    internal fun recycleGraphics() {
        mLines.forEach{
            it.recycleGraphics()
        }
    }

    internal fun getMIDIBeatTime(beat: Int): Long {
        for (f in 0 until mNumberOfMIDIBeatBlocks) {
            val (blockStartTime, midiBeatCount, nanoPerBeat) = mBeatBlocks[f]
            if (midiBeatCount <= beat && (f + 1 == mNumberOfMIDIBeatBlocks || mBeatBlocks[f + 1].midiBeatCount > beat)) {
                return (blockStartTime + nanoPerBeat * (beat - midiBeatCount)).toLong()
            }
        }
        return 0
    }
}
