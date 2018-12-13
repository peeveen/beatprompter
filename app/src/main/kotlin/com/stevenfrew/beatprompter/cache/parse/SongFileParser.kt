package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.CachedFileDescriptor
import com.stevenfrew.beatprompter.cache.parse.tag.find.*
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.song.ScrollingMode

/**
 * Base class for song file parsing.
 */
abstract class SongFileParser<TResultType>(cachedCloudFileDescriptor: CachedFileDescriptor,
                                           initialScrollMode: ScrollingMode,
                                           private val mAllowModeChange: Boolean,
                                           reportUnexpectedTags: Boolean)
    : TextFileParser<TResultType>(cachedCloudFileDescriptor, reportUnexpectedTags, DirectiveFinder, ChordFinder, ShorthandFinder) {
    protected var mOngoingBeatInfo: SongBeatInfo = SongBeatInfo(mScrollMode = initialScrollMode)
    protected var mCurrentLineBeatInfo: LineBeatInfo = LineBeatInfo(mOngoingBeatInfo)

    override fun parseLine(line: TextFileLine<TResultType>) {
        val lastLineBeatInfo = mCurrentLineBeatInfo

        val commaBars = line.mTags.filterIsInstance<BarMarkerTag>().size
        var thisScrollBeatTotalOffset = line
                .mTags
                .filterIsInstance<ScrollBeatModifierTag>()
                .sumBy { it.mModifier }

        // ... or by a tag (which overrides commas)
        val tagSequence = line.mTags.asSequence()
        val barsTag = tagSequence.filterIsInstance<BarsTag>().firstOrNull()
        val barsPerLineTag = tagSequence.filterIsInstance<BarsPerLineTag>().firstOrNull()
        val beatsPerBarTag = tagSequence.filterIsInstance<BeatsPerBarTag>().firstOrNull()
        val beatsPerMinuteTag = tagSequence.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
        val scrollBeatTag = tagSequence.filterIsInstance<ScrollBeatTag>().firstOrNull()

        val barsInThisLine = barsTag?.mBars ?: barsPerLineTag?.mBPL
        ?: if (commaBars == 0) mOngoingBeatInfo.mBPL else commaBars

        val beatsPerBarInThisLine = beatsPerBarTag?.mBPB ?: mOngoingBeatInfo.mBPB
        val beatsPerMinuteInThisLine = beatsPerMinuteTag?.mBPM ?: mOngoingBeatInfo.mBPM
        var scrollBeatInThisLine = scrollBeatTag?.mScrollBeat ?: mOngoingBeatInfo.mScrollBeat

        val previousBeatsPerBar = mOngoingBeatInfo.mBPB
        val previousScrollBeat = mOngoingBeatInfo.mScrollBeat
        // If the beats-per-bar have changed, and there is no indication of what the new scrollbeat should be,
        // set the new scrollbeat to have the same "difference" as before. For example, if the old BPB was 4,
        // and the scrollbeat was 3 (one less than BPB), a new BPB of 6 should have a scrollbeat of 5 (one
        // less than BPB)
        if ((beatsPerBarInThisLine != previousBeatsPerBar) && (scrollBeatTag == null)) {
            val prevScrollBeatDiff = previousBeatsPerBar - previousScrollBeat
            if (beatsPerBarInThisLine - prevScrollBeatDiff > 0)
                scrollBeatInThisLine = beatsPerBarInThisLine - prevScrollBeatDiff
        }
        // If this results in the new scrollbeat being off the chart, auto-fix it.
        if (scrollBeatInThisLine > beatsPerBarInThisLine)
            scrollBeatInThisLine = beatsPerBarInThisLine

        val scrollBeatTagDiff = scrollBeatInThisLine - beatsPerBarInThisLine
        thisScrollBeatTotalOffset += scrollBeatTagDiff

        if ((beatsPerBarInThisLine != 0) && (thisScrollBeatTotalOffset < -beatsPerBarInThisLine || thisScrollBeatTotalOffset >= beatsPerBarInThisLine)) {
            mErrors.add(FileParseError(line.mLineNumber, R.string.scrollbeatOffTheMap))
            thisScrollBeatTotalOffset = 0
        }

        val beatStartTags = tagSequence.filterIsInstance<BeatStartTag>().toMutableList()
        val beatStopTags = tagSequence.filterIsInstance<BeatStopTag>().toMutableList()
        val beatModeTags = listOf(beatStartTags, beatStopTags).flatten().toMutableList()

        // Multiple beatstart or beatstop tags on the same line are nonsensical
        val newScrollMode =
                if (mAllowModeChange && beatModeTags.size == 1)
                    if (beatStartTags.isNotEmpty())
                        if (mOngoingBeatInfo.mBPM == 0.0) {
                            mErrors.add(FileParseError(beatStartTags.first(), R.string.beatstart_with_no_bpm))
                            lastLineBeatInfo.mScrollMode
                        } else
                            ScrollingMode.Beat
                    else
                        ScrollingMode.Manual
                else
                    lastLineBeatInfo.mScrollMode

        val lastScrollBeatTotalOffset = lastLineBeatInfo.mScrollBeatTotalOffset

        val beatsForThisLine =
                ((beatsPerBarInThisLine * barsInThisLine)
                        - lastScrollBeatTotalOffset) + thisScrollBeatTotalOffset

        mOngoingBeatInfo = SongBeatInfo(barsPerLineTag?.mBPL
                ?: mOngoingBeatInfo.mBPL, beatsPerBarInThisLine, beatsPerMinuteInThisLine, scrollBeatInThisLine, mOngoingBeatInfo.mScrollMode)
        mCurrentLineBeatInfo = LineBeatInfo(beatsForThisLine, barsInThisLine, beatsPerBarInThisLine, beatsPerMinuteInThisLine, scrollBeatInThisLine, thisScrollBeatTotalOffset, newScrollMode)
    }

    protected data class LineBeatInfo(val mBeats: Int,
                                      val mBPL: Int,
                                      val mBPB: Int,
                                      val mBPM: Double,
                                      val mScrollBeat: Int,
                                      val mScrollBeatTotalOffset: Int,
                                      val mScrollMode: ScrollingMode = ScrollingMode.Beat) {
        constructor(songBeatInfo: SongBeatInfo) : this(songBeatInfo.mBPB * songBeatInfo.mBPL,
                songBeatInfo.mBPL,
                songBeatInfo.mBPB,
                songBeatInfo.mBPM,
                songBeatInfo.mScrollBeat,
                songBeatInfo.mBPB - songBeatInfo.mScrollBeat,
                songBeatInfo.mScrollMode)
    }

    protected data class SongBeatInfo(val mBPL: Int = 4,
                                      val mBPB: Int = 4,
                                      val mBPM: Double = 120.0,
                                      val mScrollBeat: Int = 4,
                                      val mScrollMode: ScrollingMode = ScrollingMode.Beat)
}