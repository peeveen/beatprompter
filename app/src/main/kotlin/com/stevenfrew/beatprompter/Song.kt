package com.stevenfrew.beatprompter

import android.graphics.*
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.event.AudioEvent
import com.stevenfrew.beatprompter.event.LinkedEvent
import com.stevenfrew.beatprompter.midi.BeatBlock
import com.stevenfrew.beatprompter.comm.midi.message.outgoing.OutgoingMessage

class Song(val mSongFile:SongFile, val mDisplaySettings:SongDisplaySettings,
           firstEvent: LinkedEvent, private val mLines:List<Line>, internal val mAudioEvents:List<AudioEvent>,
           val mInitialMIDIMessages:List<OutgoingMessage>, private val mBeatBlocks:List<BeatBlock>, val mSendMIDIClock:Boolean,
           val mStartScreenStrings:List<ScreenString>, val mNextSongString:ScreenString?, val mTotalStartScreenTextHeight:Int,
           val mStartedByBandLeader:Boolean, val mNextSong:String,
           val mDisplayOffset:Int, val mHeight:Int, val mScrollEndPixel:Int, val mNoScrollLines:List<Line>,
           val mBeatCounterRect:Rect, val mSongTitleHeader:ScreenString, val mSongTitleHeaderLocation:PointF) {
    internal var mCurrentLine: Line = mLines.first()
    internal var mCurrentEvent=firstEvent // Last event that executed.
    private var mNextEvent: LinkedEvent? = firstEvent.mNextEvent // Upcoming event.
    var mCancelled = false
    private val mNumberOfMIDIBeatBlocks = mBeatBlocks.size
    val mSmoothMode:Boolean = mLines.filter{it.mBeatInfo.mScrollMode==ScrollingMode.Smooth}.any()
    internal val mBackingTrack=findBackingTrack(firstEvent)

    internal fun setProgress(nano: Long) {
        val e = mCurrentEvent
        val newCurrentEvent = e.findLatestEventOnOrBefore(nano)
        mCurrentEvent = newCurrentEvent
        mNextEvent = mCurrentEvent.mNextEvent
        val newCurrentLineEvent = newCurrentEvent.mPrevLineEvent
        mCurrentLine = newCurrentLineEvent?.mLine ?: mLines.first()
    }

    internal fun getNextEvent(time: Long): LinkedEvent? {
        if (mNextEvent != null && mNextEvent!!.time <= time) {
            mCurrentEvent = mNextEvent!!
            mNextEvent = mNextEvent!!.mNextEvent
            return mCurrentEvent
        }
        return null
    }

    internal fun getTimeFromPixel(pixel: Int): Long {
        if (pixel == 0)
            return 0
        return mCurrentLine.getTimeFromPixel(pixel)
    }

    internal fun getPixelFromTime(time: Long): Int {
        if (time == 0L)
            return 0
        return mCurrentLine.getPixelFromTime(time)
    }

    internal fun recycleGraphics() {
        mLines.forEach{
            it.recycleGraphics()
        }
    }

    internal fun getMIDIBeatTime(beat: Int): Long {
        for (f in 0 until mNumberOfMIDIBeatBlocks) {
            val (blockStartTime, midiBeatCount, nanoPerBeat) = mBeatBlocks[f]
            if (midiBeatCount <= beat && (f + 1 == mNumberOfMIDIBeatBlocks || mBeatBlocks[f + 1].midiBeatCount > beat))
                return (blockStartTime + nanoPerBeat * (beat - midiBeatCount)).toLong()
        }
        return 0
    }

    companion object {
        private fun findBackingTrack(firstEvent:LinkedEvent):AudioFile?
        {
            // Find the backing track (if any)
            var thisEvent:LinkedEvent?=firstEvent
            while(thisEvent!=null)
            {
                val innerEvent=thisEvent.mEvent
                if(innerEvent is AudioEvent && innerEvent.mBackingTrack)
                    return innerEvent.mAudioFile
                thisEvent=thisEvent.mNextEvent
            }
            return null
        }
    }
}
