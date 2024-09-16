package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.parse.tag.find.ChordFinder
import com.stevenfrew.beatprompter.cache.parse.tag.find.DirectiveFinder
import com.stevenfrew.beatprompter.cache.parse.tag.find.ShorthandFinder
import com.stevenfrew.beatprompter.cache.parse.tag.song.AudioTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BarMarkerTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BarsPerLineTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BarsTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatStartTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatStopTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatsPerBarTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatsPerMinuteTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.EndOfVariationExclusionTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ScrollBeatModifierTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ScrollBeatTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.StartOfVariationExclusionTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.VariationsTag
import com.stevenfrew.beatprompter.song.ScrollingMode

/**
 * Base class for song file parsing.
 */
abstract class SongFileParser<TResultType>(
  cachedCloudFile: CachedFile,
  initialScrollMode: ScrollingMode,
  private val allowModeChange: Boolean,
  protected val variation: String?,
  reportUnexpectedTags: Boolean
) : TextFileParser<TResultType>(
  cachedCloudFile,
  reportUnexpectedTags,
  DirectiveFinder,
  ChordFinder,
  ShorthandFinder
) {
  protected var ongoingBeatInfo: SongBeatInfo = SongBeatInfo(scrollMode = initialScrollMode)
  protected var currentLineBeatInfo: LineBeatInfo = LineBeatInfo(ongoingBeatInfo)

  // Audio files are now a 2D array ... list of audio files per variation.
  protected val variationAudioTags = mutableMapOf<String, MutableList<AudioTag>>()
  protected val variations = mutableListOf<String>()

  protected val variationExclusions: ArrayDeque<List<String>> = ArrayDeque()

  override fun parseLine(line: TextFileLine<TResultType>): Boolean {
    val lastLineBeatInfo = currentLineBeatInfo

    val commaBars = line.tags.filterIsInstance<BarMarkerTag>().size
    var thisScrollBeatTotalOffset = line
      .tags
      .filterIsInstance<ScrollBeatModifierTag>()
      .sumOf { it.modifier }

    // ... or by a tag (which overrides commas)
    val tagSequence = line.tags.asSequence()

    // We're going to check if we're in a variation exclusion section.
    // This section MIGHT be defined as a one-line exclusion, with varxstart/varxstop on the same line.
    // So before we check, we need to look for varxstart tags.
    val variationExclusionStartTag =
      tagSequence.filterIsInstance<StartOfVariationExclusionTag>().firstOrNull()
    if (variationExclusionStartTag != null)
      variationExclusions.add(variationExclusionStartTag.variations)
    // Are we in a variation exclusion section?
    // Does it instruct us to exclude this line for the current variation?
    val excludeLine =
      variationExclusions.any { it.contains(variation ?: variations.firstOrNull() ?: "") }
    // Now that we've figured out whether this is an exclusion section, we need to look for varxstop tags
    // on this line.
    val variationExclusionEndTag =
      tagSequence.filterIsInstance<EndOfVariationExclusionTag>().firstOrNull()
    if (variationExclusionEndTag != null)
      variationExclusions.removeLast()
    // If we're in an exclusion section, bail out. We do not process any other tags or text.
    if (excludeLine)
      return false

    val barsTag = tagSequence.filterIsInstance<BarsTag>().firstOrNull()
    val barsPerLineTag = tagSequence.filterIsInstance<BarsPerLineTag>().firstOrNull()
    val beatsPerBarTag = tagSequence.filterIsInstance<BeatsPerBarTag>().firstOrNull()
    val beatsPerMinuteTag = tagSequence.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
    val scrollBeatTag = tagSequence.filterIsInstance<ScrollBeatTag>().firstOrNull()
    val audioTags = tagSequence.filterIsInstance<AudioTag>()
    val variationsTags = tagSequence.filterIsInstance<VariationsTag>()

    // Variations can only be defined once.
    if (variationsTags.any()) {
      if (variations.isEmpty()) {
        variations.addAll(variationsTags.flatMap { it.variations })
        variations.forEach {
          variationAudioTags[it] = mutableListOf()
        }
      } else
        errors.add(FileParseError(line.lineNumber, R.string.variationsAlreadyDefined))
    }

    // Each audio file defined on a line now maps to a variation.
    // The audio filename itself can be a variation name is no explicitly-named variations
    // have been defined. Otherwise, they are mapped to variation by index.
    val noVariationsDefined = variations.isEmpty()
    audioTags.forEachIndexed { index, it ->
      val variationNames = getVariationNamesForIndexedAudioFile(
        index,
        audioTags.count(),
        line.lineNumber,
        it.filename,
        noVariationsDefined
      )
      variationNames?.forEach { variationName -> variationAudioTags[variationName]?.add(it) }
    }

    // Commas take precedence.
    val barsInThisLine = if (commaBars == 0) barsTag?.bars ?: barsPerLineTag?.bpl
    ?: ongoingBeatInfo.bpl else commaBars

    val beatsPerBarInThisLine = beatsPerBarTag?.bpb ?: ongoingBeatInfo.bpb
    val beatsPerMinuteInThisLine = beatsPerMinuteTag?.bpm ?: ongoingBeatInfo.bpm
    var scrollBeatInThisLine = scrollBeatTag?.scrollBeat ?: ongoingBeatInfo.scrollBeat

    val previousBeatsPerBar = ongoingBeatInfo.bpb
    val previousScrollBeat = ongoingBeatInfo.scrollBeat
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
      errors.add(FileParseError(line.lineNumber, R.string.scrollbeatOffTheMap))
      thisScrollBeatTotalOffset = 0
    }

    val beatStartTags = tagSequence.filterIsInstance<BeatStartTag>().toMutableList()
    val beatStopTags = tagSequence.filterIsInstance<BeatStopTag>().toMutableList()
    val beatModeTags = listOf(beatStartTags, beatStopTags).flatten().toMutableList()

    // Multiple beatstart or beatstop tags on the same line are nonsensical
    val newScrollMode =
      if (allowModeChange && beatModeTags.size == 1)
        if (beatStartTags.isNotEmpty())
          if (ongoingBeatInfo.bpm == 0.0) {
            errors.add(FileParseError(beatStartTags.first(), R.string.beatstart_with_no_bpm))
            lastLineBeatInfo.scrollMode
          } else
            ScrollingMode.Beat
        else
          ScrollingMode.Manual
      else
        lastLineBeatInfo.scrollMode

    // If this line is not in Beat mode, then the scrollbeat offset stops here.
    if (newScrollMode !== ScrollingMode.Beat)
      thisScrollBeatTotalOffset = 0

    val lastScrollBeatTotalOffset = lastLineBeatInfo.scrollBeatTotalOffset

    val beatsForThisLine =
      ((beatsPerBarInThisLine * barsInThisLine)
        - lastScrollBeatTotalOffset) + thisScrollBeatTotalOffset

    ongoingBeatInfo = SongBeatInfo(
      barsPerLineTag?.bpl
        ?: ongoingBeatInfo.bpl,
      beatsPerBarInThisLine,
      beatsPerMinuteInThisLine,
      scrollBeatInThisLine,
      ongoingBeatInfo.scrollMode
    )
    currentLineBeatInfo = LineBeatInfo(
      beatsForThisLine,
      barsInThisLine,
      beatsPerBarInThisLine,
      beatsPerMinuteInThisLine,
      scrollBeatInThisLine,
      thisScrollBeatTotalOffset,
      lastScrollBeatTotalOffset,
      newScrollMode
    )
    return true
  }

  private fun getVariationNamesForIndexedAudioFile(
    index: Int,
    audioTagCount: Int,
    lineNumber: Int,
    filename: String,
    canAddVariation: Boolean
  ): List<String>? {
    if (variations.size > index)
      return if (audioTagCount > index + 1)
        listOf(variations[index])
      else
        variations.takeLast(variations.size - index)
    if (canAddVariation) {
      variations.add(filename)
      variationAudioTags[filename] = mutableListOf()
      return listOf(filename)
    }
    errors.add(FileParseError(lineNumber, R.string.tooManyAudioTags))
    return null
  }

  protected data class LineBeatInfo(
    val beats: Int,
    val bpl: Int,
    val bpb: Int,
    val bpm: Double,
    val scrollBeat: Int,
    val scrollBeatTotalOffset: Int,
    val lastScrollBeatTotalOffset: Int,
    val scrollMode: ScrollingMode = ScrollingMode.Beat
  ) {
    constructor(songBeatInfo: SongBeatInfo) : this(
      songBeatInfo.bpb * songBeatInfo.bpl,
      songBeatInfo.bpl,
      songBeatInfo.bpb,
      songBeatInfo.bpm,
      songBeatInfo.scrollBeat,
      songBeatInfo.bpb - songBeatInfo.scrollBeat,
      0,
      songBeatInfo.scrollMode
    )
  }

  protected data class SongBeatInfo(
    val bpl: Int = 4,
    val bpb: Int = 4,
    val bpm: Double = 120.0,
    val scrollBeat: Int = 4,
    val scrollMode: ScrollingMode = ScrollingMode.Beat
  )
}