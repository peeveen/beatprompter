package com.stevenfrew.beatprompter

import android.graphics.*
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.event.AudioEvent
import com.stevenfrew.beatprompter.event.BaseEvent
import com.stevenfrew.beatprompter.midi.BeatBlock
import com.stevenfrew.beatprompter.midi.OutgoingMessage

class Song(val mSongFile:SongFile, val mDisplaySettings:SongDisplaySettings, val mSongHeight:Int,
           firstEvent:BaseEvent, private val mLines:List<Line>, internal val mAudioEvents:List<AudioEvent>,
           val mInitialMIDIMessages:List<OutgoingMessage>, private val mBeatBlocks:List<BeatBlock>, val mSendMIDIClock:Boolean,
           val mStartScreenStrings:List<ScreenString>, val mNextSongString:ScreenString?, val mTotalStartScreenTextHeight:Int,
           val mStartedByBandLeader:Boolean, val mNextSong:String,
           val mSmoothScrollOffset:Int, val mBeatCounterRect:Rect, val mSongTitleHeader:ScreenString, val mSongTitleHeaderLocation:PointF) {
    internal var mCurrentLine: Line? = mLines.firstOrNull()
    internal var mCurrentEvent=firstEvent // Last event that executed.
    private var mNextEvent: BaseEvent? = firstEvent.mNextEvent // Upcoming event.
    var mCancelled = false
    private val mNumberOfMIDIBeatBlocks = mBeatBlocks.size
    val mScrollMode:SongScrollingMode

    init {
        val containsBeatLines=mLines.filter{it.mLineScrollMode==LineScrollingMode.Beat}.any()
        val containsSmoothLines=mLines.filter{it.mLineScrollMode==LineScrollingMode.Smooth}.any()
        val containsManualLines=mLines.filter{it.mLineScrollMode==LineScrollingMode.Manual}.any()
        mScrollMode = when {
            arrayOf(containsBeatLines,containsSmoothLines,containsManualLines).count{it==true}>1 -> SongScrollingMode.Mixed
            containsBeatLines -> SongScrollingMode.Beat
            containsSmoothLines -> SongScrollingMode.Smooth
            else -> SongScrollingMode.Manual
        }
    }

    internal fun setProgress(nano: Long) {
        val e = mCurrentEvent
        val newCurrentEvent = e.findEventOnOrBefore(nano)
        mCurrentEvent = newCurrentEvent
        mNextEvent = mCurrentEvent.mNextEvent
        val newCurrentLineEvent = newCurrentEvent.mPrevLineEvent
        mCurrentLine = newCurrentLineEvent?.mLine ?: mLines.firstOrNull()
    }

    internal fun getNextEvent(time: Long): BaseEvent? {
        if (mNextEvent != null && mNextEvent!!.mEventTime <= time) {
            mCurrentEvent = mNextEvent!!
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
