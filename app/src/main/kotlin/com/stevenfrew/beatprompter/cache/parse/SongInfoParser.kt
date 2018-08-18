package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.LineBeatInfo
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ScrollingMode
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.ImageFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.cache.parse.tag.song.*

class SongInfoParser constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor,currentAudioFiles:List<AudioFile>,currentImageFiles:List<ImageFile>):SongFileParser<SongFile>(cachedCloudFileDescriptor,currentAudioFiles,currentImageFiles) {
    override fun parseLine(line: TextFileLine<SongFile>) {
        // Bars can be defined by commas ....
        var commaBars:Int?=null
        // TODO: Deep clone of tags
        val tags=line.mTags.toList()

        var workLine=line.mTaglessLine
        while (workLine.startsWith(",")) {
            if(commaBars==null)
                commaBars=0
            workLine = workLine.substring(1)
            tags.forEach{it.retreatFrom(0)}
            commaBars++
        }

        var scrollBeatOffset=0
        while (workLine.endsWith(">") || workLine.endsWith("<")) {
            if (workLine.endsWith(">"))
                scrollBeatOffset++
            else if (workLine.endsWith("<"))
                scrollBeatOffset--
            workLine = workLine.substring(0, workLine.length - 1)
            tags.forEach{it.retreatFrom(workLine.length)}
        }

        // TODO: dynamic BPB changing

        // ... or by a tag (which overrides commas)
        val barsTag=tags.filterIsInstance<BarsTag>().firstOrNull()
        val barsPerLineTag=tags.filterIsInstance<BarsPerLineTag>().firstOrNull()
        val beatsPerBarTag=tags.filterIsInstance<BeatsPerBarTag>().firstOrNull()
        val beatsPerMinuteTag=tags.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
        val scrollBeatTag=tags.filterIsInstance<ScrollBeatTag>().firstOrNull()

        val beatStartTags=tags.filterIsInstance<BeatStartTag>()
        val beatStopTags=tags.filterIsInstance<BeatStopTag>()
        val beatModeTags=listOf(beatStartTags,beatStopTags).flatMap { it }

        val barsInThisLine=barsTag?.mBars?:barsPerLineTag?.mBPL?:commaBars?:mBeatInfo.mBPL
        val beatsPerBarInThisLine=beatsPerBarTag?.mBPB?:mBeatInfo.mBPB
        val beatsPerMinuteInThisLine=beatsPerMinuteTag?.mBPM?:mBeatInfo.mBPM
        var scrollBeatInThisLine=scrollBeatTag?.mScrollBeat?:mBeatInfo.mScrollBeat
        var modeOnThisLine=mBeatInfo.mScrollingMode

        // Multiple beatstart or beatstop tags on the same line are nonsensical
        if(beatModeTags.size>1)
            mErrors.add(FileParseError(beatModeTags.first(), BeatPrompterApplication.getResourceString(R.string.multiple_beatstart_beatstop_same_line)))
        else if(beatModeTags.size==1)
            if(beatStartTags.isNotEmpty())
                if(beatsPerMinuteInThisLine==0.0)
                    mErrors.add(FileParseError(beatStartTags.first(), BeatPrompterApplication.getResourceString(R.string.beatstart_with_no_bpm)))
                else
                    modeOnThisLine= ScrollingMode.Beat
            else
                modeOnThisLine = ScrollingMode.Manual

        val previousBeatsPerBar=mBeatInfo.mBPB
        val previousScrollBeat=mBeatInfo.mScrollBeat
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

        mBeatInfo= LineBeatInfo(barsPerLineTag?.mBPL?:mBeatInfo.mBPL,beatsPerBarInThisLine,beatsPerMinuteInThisLine,scrollBeatInThisLine,scrollBeatOffset,modeOnThisLine)

        if ((beatsPerBarInThisLine!=0)&&(scrollBeatOffset < -beatsPerBarInThisLine || scrollBeatOffset >= beatsPerBarInThisLine)) {
            mErrors.add(FileParseError(line.mLineNumber, BeatPrompterApplication.getResourceString(R.string.scrollbeatOffTheMap)))
            scrollBeatOffset = 0
        }

        thisline.mBeatInfo= LineBeatInfo(barsInThisLine,beatsPerBarInThisLine,beatsPerMinuteInThisLine,scrollBeatInThisLine,scrollBeatOffset,modeOnThisLine)
    }

    override fun getResult(): SongFile {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return SongFile(mCachedCloudFileDescriptor,"",listOf(),listOf())
    }
}
