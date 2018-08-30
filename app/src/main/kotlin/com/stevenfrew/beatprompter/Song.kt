package com.stevenfrew.beatprompter

import android.graphics.*
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.event.AudioEvent
import com.stevenfrew.beatprompter.event.BaseEvent
import com.stevenfrew.beatprompter.event.LineEvent
import com.stevenfrew.beatprompter.midi.BeatBlock
import com.stevenfrew.beatprompter.midi.OutgoingMessage

class Song(val mSongFile:SongFile, val mDisplaySettings:SongDisplaySettings, val mSongHeight:Int,
           firstEvent:BaseEvent, private val mLines:List<Line>, lineEvents:List<LineEvent>, internal val mAudioEvents:List<AudioEvent>,
           val mInitialMIDIMessages:List<OutgoingMessage>, private val mBeatBlocks:List<BeatBlock>, val mSendMIDIClock:Boolean,
           val mStartScreenStrings:List<ScreenString>, val mNextSongString:ScreenString?, val mTotalStartScreenTextHeight:Int,
           val mStartedByBandLeader:Boolean, val mNextSong:String,
           val mSmoothScrollOffset:Int, val mBeatCounterRect:Rect, val mSongTitleHeader:ScreenString, val mSongTitleHeaderLocation:PointF) {
    internal var mCurrentLine: Line? = mLines.firstOrNull()
    internal var mCurrentEvent=firstEvent // Last event that executed.
    private var mNextEvent: BaseEvent? = firstEvent.mNextEvent // Upcoming event.
    internal val mNoScrollLines:List<Line>
    var mCancelled = false
    private val mNumberOfMIDIBeatBlocks = mBeatBlocks.size
    val mScrollMode:SongScrollingMode
    internal val mBackingTrack: AudioFile?
    internal val mSmoothScrollEndOffset:Int

    init {
        val containsBeatLines=mLines.filter{it.mBeatInfo.mScrollMode==LineScrollingMode.Beat}.any()
        val containsSmoothLines=mLines.filter{it.mBeatInfo.mScrollMode==LineScrollingMode.Smooth}.any()
        val containsManualLines=mLines.filter{it.mBeatInfo.mScrollMode==LineScrollingMode.Manual}.any()
        mScrollMode = when {
            listOf(containsBeatLines,containsSmoothLines,containsManualLines).count{it}>1 -> SongScrollingMode.Mixed
            containsBeatLines -> SongScrollingMode.Beat
            containsSmoothLines -> SongScrollingMode.Smooth
            else -> SongScrollingMode.Manual
        }
        val noScrollLines=mutableListOf<Line>()
        val lastLineIsBeat=mLines.lastOrNull()?.mBeatInfo?.mScrollMode==LineScrollingMode.Beat
        var smoothScrollEndOffset=0
        if(lastLineIsBeat) {
            noScrollLines.add(mLines.last())
            lineEvents.lastOrNull()?.remove()
        }
        else if(containsSmoothLines)
        {
            var availableScreenHeight=mDisplaySettings.mScreenSize.height()-(mBeatCounterRect.height()+mSmoothScrollOffset)
            for(lineEvent in lineEvents.reversed())
            {
                availableScreenHeight-=lineEvent.mLine.mMeasurements.mLineHeight
                if(availableScreenHeight>=0) {
                    noScrollLines.add(lineEvent.mLine)
                    lineEvent.remove()
                }
                else {
                    smoothScrollEndOffset=availableScreenHeight+lineEvent.mLine.mMeasurements.mLineHeight
                    break
                }
            }
        }
        mSmoothScrollEndOffset=smoothScrollEndOffset
        mNoScrollLines=noScrollLines
        mBackingTrack=findBackingTrack(firstEvent)
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
            if (midiBeatCount <= beat && (f + 1 == mNumberOfMIDIBeatBlocks || mBeatBlocks[f + 1].midiBeatCount > beat))
                return (blockStartTime + nanoPerBeat * (beat - midiBeatCount)).toLong()
        }
        return 0
    }

    companion object {
        private fun findBackingTrack(firstEvent:BaseEvent):AudioFile?
        {
            // Find the backing track (if any)
            var thisEvent:BaseEvent?=firstEvent
            while(thisEvent!=null)
            {
                if(thisEvent is AudioEvent && thisEvent.mBackingTrack)
                    return thisEvent.mAudioFile
                thisEvent=thisEvent.mNextEvent
            }
            return null
        }
    }
}
