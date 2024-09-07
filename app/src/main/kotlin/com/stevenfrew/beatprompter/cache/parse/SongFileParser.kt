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
import com.stevenfrew.beatprompter.cache.parse.tag.song.ScrollBeatModifierTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ScrollBeatTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.VariationsTag
import com.stevenfrew.beatprompter.song.ScrollingMode

/**
 * Base class for song file parsing.
 */
abstract class SongFileParser<TResultType>(
	cachedCloudFile: CachedFile,
	initialScrollMode: ScrollingMode,
	private val allowModeChange: Boolean,
	reportUnexpectedTags: Boolean
) : TextFileParser<TResultType>(
	cachedCloudFile,
	reportUnexpectedTags,
	DirectiveFinder,
	ChordFinder,
	ShorthandFinder
) {
	protected var ongoingBeatInfo: SongBeatInfo = SongBeatInfo(mScrollMode = initialScrollMode)
	protected var currentLineBeatInfo: LineBeatInfo = LineBeatInfo(ongoingBeatInfo)

	// Audio files are now a 2D array ... list of audio files per variation.
	protected val audioFiles = mutableMapOf<String, MutableList<String>>()
	protected val variations = mutableListOf<String>()

	override fun parseLine(line: TextFileLine<TResultType>) {
		val lastLineBeatInfo = currentLineBeatInfo

		val commaBars = line.tags.filterIsInstance<BarMarkerTag>().size
		var thisScrollBeatTotalOffset = line
			.tags
			.filterIsInstance<ScrollBeatModifierTag>()
			.sumOf { it.modifier }

		// ... or by a tag (which overrides commas)
		val tagSequence = line.tags.asSequence()
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
					audioFiles[it] = mutableListOf()
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
			variationNames?.forEach { variationName -> audioFiles[variationName]?.add(it.filename) }
		}

		// Commas take precedence.
		val barsInThisLine = if (commaBars == 0) barsTag?.bars ?: barsPerLineTag?.bpl
		?: ongoingBeatInfo.mBPL else commaBars

		val beatsPerBarInThisLine = beatsPerBarTag?.bpb ?: ongoingBeatInfo.mBPB
		val beatsPerMinuteInThisLine = beatsPerMinuteTag?.bpm ?: ongoingBeatInfo.mBPM
		var scrollBeatInThisLine = scrollBeatTag?.scrollBeat ?: ongoingBeatInfo.mScrollBeat

		val previousBeatsPerBar = ongoingBeatInfo.mBPB
		val previousScrollBeat = ongoingBeatInfo.mScrollBeat
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
					if (ongoingBeatInfo.mBPM == 0.0) {
						errors.add(FileParseError(beatStartTags.first(), R.string.beatstart_with_no_bpm))
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

		ongoingBeatInfo = SongBeatInfo(
			barsPerLineTag?.bpl
				?: ongoingBeatInfo.mBPL,
			beatsPerBarInThisLine,
			beatsPerMinuteInThisLine,
			scrollBeatInThisLine,
			ongoingBeatInfo.mScrollMode
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
	}

	private fun getVariationNamesForIndexedAudioFile(
		index: Int,
		audioTagCount: Int,
		lineNumber: Int,
		filename: String,
		canAddVariation: Boolean
	): List<String>? {
		if (variations.size > index)
			return if (audioTagCount > index)
				listOf(variations[index])
			else
				variations.takeLast(variations.size - index)
		if (canAddVariation) {
			variations.add(filename)
			audioFiles[filename] = mutableListOf()
			return listOf(filename)
		}
		errors.add(FileParseError(lineNumber, R.string.tooManyAudioTags))
		return null
	}

	protected data class LineBeatInfo(
		val mBeats: Int,
		val mBPL: Int,
		val mBPB: Int,
		val mBPM: Double,
		val mScrollBeat: Int,
		val mScrollBeatTotalOffset: Int,
		val mLastScrollBeatTotalOffset: Int,
		val mScrollMode: ScrollingMode = ScrollingMode.Beat
	) {
		constructor(songBeatInfo: SongBeatInfo) : this(
			songBeatInfo.mBPB * songBeatInfo.mBPL,
			songBeatInfo.mBPL,
			songBeatInfo.mBPB,
			songBeatInfo.mBPM,
			songBeatInfo.mScrollBeat,
			songBeatInfo.mBPB - songBeatInfo.mScrollBeat,
			0,
			songBeatInfo.mScrollMode
		)
	}

	protected data class SongBeatInfo(
		val mBPL: Int = 4,
		val mBPB: Int = 4,
		val mBPM: Double = 120.0,
		val mScrollBeat: Int = 4,
		val mScrollMode: ScrollingMode = ScrollingMode.Beat
	)
}