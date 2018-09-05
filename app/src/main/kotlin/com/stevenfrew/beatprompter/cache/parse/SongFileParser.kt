package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.find.*
import com.stevenfrew.beatprompter.cache.parse.tag.song.*

abstract class SongFileParser<TResultType> constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, initialScrollMode: ScrollingMode, private val mAllowModeChange:Boolean):TextFileParser<TResultType>(cachedCloudFileDescriptor, DirectiveFinder, ChordFinder, ShorthandFinder) {
    protected var mOngoingBeatInfo:SongBeatInfo=SongBeatInfo(mScrollMode=initialScrollMode)
    protected var mCurrentLineBeatInfo:LineBeatInfo=LineBeatInfo(mOngoingBeatInfo)

    override fun parseTag(foundTag: FoundTag,lineNumber:Int):Tag
    {
        val txt=foundTag.mText
        val colonIndex = txt.indexOf(":")
        val spaceIndex = txt.indexOf(" ")
        val realIndex=if(colonIndex==-1) spaceIndex else colonIndex
        val name:String
        val value:String
        if (realIndex == -1) {
            name = if (foundTag.mType== TagType.Chord) txt else txt.toLowerCase()
            value = ""
        } else {
            name = if (foundTag.mType== TagType.Chord) txt else txt.substring(0, realIndex).toLowerCase()
            value = txt.substring(realIndex + 1).trim()
       }
        if(foundTag.mType== TagType.Chord)
            return ChordTag(name, lineNumber, foundTag.mStart)

        return when(name)
        {
            // Beat and audio info is always relevant.
            "b", "bars"-> BarsTag(name, lineNumber, foundTag.mStart, value)
            "," -> BarMarkerTag(lineNumber,foundTag.mStart)
            "bpm", "metronome", "beatsperminute"-> BeatsPerMinuteTag(name, lineNumber, foundTag.mStart, value)
            "bpb", "beatsperbar"-> BeatsPerBarTag(name, lineNumber, foundTag.mStart, value)
            "bpl", "barsperline"-> BarsPerLineTag(name, lineNumber, foundTag.mStart, value)
            ">", "<" -> ScrollBeatModifierTag(name, lineNumber,foundTag.mStart)
            "scrollbeat", "sb"-> ScrollBeatTag(name, lineNumber, foundTag.mStart, value)
            "beatstart"-> BeatStartTag(name, lineNumber, foundTag.mStart)
            "beatstop"-> BeatStopTag(name, lineNumber, foundTag.mStart)
            "track", "audio", "musicpath"->return AudioTag(name,lineNumber,foundTag.mStart,value)
            else -> createSongTag(name,lineNumber,foundTag.mStart,value)
        }
    }

    abstract fun createSongTag(name:String,lineNumber:Int,position:Int,value:String):Tag

    override fun parseLine(line: TextFileLine<TResultType>) {
        val lastLineBeatInfo=mCurrentLineBeatInfo

        val commaBars=line.mTags.filterIsInstance<BarMarkerTag>().size
        val scrollBeatModifiers=line.mTags.filterIsInstance<ScrollBeatModifierTag>()
        var scrollBeatOffset=scrollBeatModifiers.sumBy{it.mModifier}

        // ... or by a tag (which overrides commas)
        val barsTag=line.mTags.filterIsInstance<BarsTag>().firstOrNull()
        val barsPerLineTag=line.mTags.filterIsInstance<BarsPerLineTag>().firstOrNull()
        val beatsPerBarTag=line.mTags.filterIsInstance<BeatsPerBarTag>().firstOrNull()
        val beatsPerMinuteTag=line.mTags.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
        val scrollBeatTag=line.mTags.filterIsInstance<ScrollBeatTag>().firstOrNull()

        val barsInThisLine=barsTag?.mBars?:barsPerLineTag?.mBPL?: if(commaBars==0) mOngoingBeatInfo.mBPL else commaBars
        val beatsPerBarInThisLine=beatsPerBarTag?.mBPB?:mOngoingBeatInfo.mBPB
        val beatsPerMinuteInThisLine=beatsPerMinuteTag?.mBPM?:mOngoingBeatInfo.mBPM
        var scrollBeatInThisLine=scrollBeatTag?.mScrollBeat?:mOngoingBeatInfo.mScrollBeat

        val previousBeatsPerBar=mOngoingBeatInfo.mBPB
        val previousScrollBeat=mOngoingBeatInfo.mScrollBeat
        // If the beats-per-bar have changed, and there is no indication of what the new scrollbeat should be,
        // set the new scrollbeat to have the same "difference" as before. For example, if the old BPB was 4,
        // and the scrollbeat was 3 (one less than BPB), a new BPB of 6 should have a scrollbeat of 5 (one
        // less than BPB)
        if((beatsPerBarInThisLine!=previousBeatsPerBar)&&(scrollBeatTag==null))
        {
            val prevScrollBeatDiff = previousBeatsPerBar - previousScrollBeat
            if(beatsPerBarInThisLine-prevScrollBeatDiff>0)
                scrollBeatInThisLine=beatsPerBarInThisLine-prevScrollBeatDiff
        }
        if(scrollBeatInThisLine>beatsPerBarInThisLine)
            scrollBeatInThisLine=beatsPerBarInThisLine

        if ((beatsPerBarInThisLine!=0)&&(scrollBeatOffset < -beatsPerBarInThisLine || scrollBeatOffset >= beatsPerBarInThisLine)) {
            mErrors.add(FileParseError(line.mLineNumber, BeatPrompterApplication.getResourceString(R.string.scrollbeatOffTheMap)))
            scrollBeatOffset = 0
        }

        val beatStartTags=line.mTags.filterIsInstance<BeatStartTag>().toMutableList()
        val beatStopTags=line.mTags.filterIsInstance<BeatStopTag>().toMutableList()
        val beatModeTags=listOf(beatStartTags,beatStopTags).flatMap { it }.toMutableList()

        // Multiple beatstart or beatstop tags on the same line are nonsensical
        val newScrollMode=
            if(mAllowModeChange && beatModeTags.size==1)
                if(beatStartTags.isNotEmpty())
                    if(mOngoingBeatInfo.mBPM==0.0) {
                        mErrors.add(FileParseError(beatStartTags.first(), BeatPrompterApplication.getResourceString(R.string.beatstart_with_no_bpm)))
                        lastLineBeatInfo.mScrollMode
                    }
                    else
                        ScrollingMode.Beat
                else
                    ScrollingMode.Manual
            else
                lastLineBeatInfo.mScrollMode

        val lastScrollBeatOffset = lastLineBeatInfo.mScrollBeatOffset
        val lastBPB = lastLineBeatInfo.mBPB
        val lastScrollBeat = lastLineBeatInfo.mScrollBeat
        val scrollBeatDifference =mCurrentLineBeatInfo.mScrollBeat - mCurrentLineBeatInfo.mBPB - (lastScrollBeat - lastBPB)

        var beatsForThisLine = beatsPerBarInThisLine * barsInThisLine
        beatsForThisLine += scrollBeatOffset
        beatsForThisLine += scrollBeatDifference
        beatsForThisLine -= lastScrollBeatOffset

        mOngoingBeatInfo=SongBeatInfo(barsPerLineTag?.mBPL?:mOngoingBeatInfo.mBPL,beatsPerBarInThisLine,beatsPerMinuteInThisLine,scrollBeatInThisLine,mOngoingBeatInfo.mScrollMode)
        mCurrentLineBeatInfo= LineBeatInfo(beatsForThisLine,barsInThisLine,beatsPerBarInThisLine,beatsPerMinuteInThisLine,scrollBeatInThisLine,scrollBeatOffset,newScrollMode)
    }
}