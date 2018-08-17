package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.LineBeatInfo
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ScrollingMode
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.ImageFile
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.midi.SongTrigger

class SongFileLine(line:String, lineNumber:Int, parsingState:SongParsingState):FileLine<SongParsingState>(line,lineNumber,parsingState) {
    val mBeatInfo: LineBeatInfo
    val chordTags :List<Tag>
        get()= mTags.filter{it is ChordTag }
    val nonChordTags :List<Tag>
        get()= mTags.filterNot{it is ChordTag }

    init {
        // Bars can be defined by commas ....
        var commaBars:Int?=null
        while (mTaglessLine.startsWith(",")) {
            if(commaBars==null)
                commaBars=0
            mTaglessLine = mTaglessLine.substring(1)
            mTags.forEach{it.retreatFrom(0)}
            commaBars++
        }

        var scrollBeatOffset=0
        while (mTaglessLine.endsWith(">") || mTaglessLine.endsWith("<")) {
            if (mTaglessLine.endsWith(">"))
                scrollBeatOffset++
            else if (mTaglessLine.endsWith("<"))
                scrollBeatOffset--
            mTaglessLine = mTaglessLine.substring(0, mTaglessLine.length - 1)
            mTags.forEach{it.retreatFrom(mTaglessLine.length)}
        }

        // TODO: dynamic BPB changing

        // ... or by a tag (which overrides commas)
        val barsTag=mTags.filterIsInstance<BarsTag>().firstOrNull()
        val barsPerLineTag=mTags.filterIsInstance<BarsPerLineTag>().firstOrNull()
        val beatsPerBarTag=mTags.filterIsInstance<BeatsPerBarTag>().firstOrNull()
        val beatsPerMinuteTag=mTags.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
        val scrollBeatTag=mTags.filterIsInstance<ScrollBeatTag>().firstOrNull()

        val beatStartTags=mTags.filterIsInstance<BeatStartTag>()
        val beatStopTags=mTags.filterIsInstance<BeatStopTag>()
        val beatModeTags=listOf(beatStartTags,beatStopTags).flatMap { it }

        val barsInThisLine=barsTag?.mBars?:barsPerLineTag?.mBPL?:commaBars?:parsingState.mBeatInfo.mBPL
        val beatsPerBarInThisLine=beatsPerBarTag?.mBPB?:parsingState.mBeatInfo.mBPB
        val beatsPerMinuteInThisLine=beatsPerMinuteTag?.mBPM?:parsingState.mBeatInfo.mBPM
        var scrollBeatInThisLine=scrollBeatTag?.mScrollBeat?:parsingState.mBeatInfo.mScrollBeat
        var modeOnThisLine=parsingState.mBeatInfo.mScrollingMode

        // Multiple beatstart or beatstop tags on the same line are nonsensical
        if(beatModeTags.size>1)
            parsingState.mErrors.add(FileParseError(beatModeTags.first(), BeatPrompterApplication.getResourceString(R.string.multiple_beatstart_beatstop_same_line)))
        else if(beatModeTags.size==1)
            if(beatStartTags.isNotEmpty())
                if(beatsPerMinuteInThisLine==0.0)
                    parsingState.mErrors.add(FileParseError(beatStartTags.first(), BeatPrompterApplication.getResourceString(R.string.beatstart_with_no_bpm)))
                else
                    modeOnThisLine= ScrollingMode.Beat
            else
                modeOnThisLine = ScrollingMode.Manual

        val previousBeatsPerBar=parsingState.mBeatInfo.mBPB
        val previousScrollBeat=parsingState.mBeatInfo.mScrollBeat
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

        parsingState.mBeatInfo=LineBeatInfo(barsPerLineTag?.mBPL?:parsingState.mBeatInfo.mBPL,beatsPerBarInThisLine,beatsPerMinuteInThisLine,scrollBeatInThisLine,scrollBeatOffset,modeOnThisLine)

        if ((beatsPerBarInThisLine!=0)&&(scrollBeatOffset < -beatsPerBarInThisLine || scrollBeatOffset >= beatsPerBarInThisLine)) {
            parsingState.mErrors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.scrollbeatOffTheMap)))
            scrollBeatOffset = 0
        }

        mBeatInfo= LineBeatInfo(barsInThisLine,beatsPerBarInThisLine,beatsPerMinuteInThisLine,scrollBeatInThisLine,scrollBeatOffset,modeOnThisLine)
    }

    override fun parseTag(text:String,lineNumber:Int,position:Int,parsingState:SongParsingState):Tag
    {
        return Tag.parseSongTag(text,lineNumber,position,parsingState)
    }

    fun getTitle(): String? {
        return mTags.filterIsInstance<TitleTag>().firstOrNull()?.mTitle
    }

    fun getKey(): String? {
        return mTags.filterIsInstance<KeyTag>().firstOrNull()?.mKey
    }

    private fun getFirstChordTag(): Tag? {
        return mTags.filterIsInstance<ChordTag>().firstOrNull {it.isValidChord()}
    }

    fun getFirstChord(): String? {
        return getFirstChordTag()?.mName
    }

    fun getArtist(): String? {
        return mTags.filterIsInstance<ArtistTag>().firstOrNull()?.mArtist
    }

    fun getBPM(): Double? {
        return mTags.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()?.mBPM
    }

    fun getMIDISongSelectTrigger(): SongTrigger? {
        return mTags.filterIsInstance<MIDISongSelectTriggerTag>().firstOrNull()?.mTrigger
    }

    fun getMIDIProgramChangeTrigger(): SongTrigger? {
        return mTags.filterIsInstance<MIDIProgramChangeTriggerTag>().firstOrNull()?.mTrigger
    }

    fun getAudioFiles(): List<AudioFile> {
        return mTags.filterIsInstance<TrackTag>().map{it.mAudioFile}
    }

    fun getImageFiles(): List<ImageFile> {
        return mTags.filterIsInstance<ImageTag>().map{it.mImageFile}
    }
}