package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.cache.parse.tag.song.ArtistTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.AudioTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BarMarkerTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BarsPerLineTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BarsTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatStartTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatStopTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatsPerBarTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatsPerMinuteTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ChordTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.CommentTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.CountTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.EndOfHighlightTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.FilterOnlyTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ImageTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.KeyTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.LegacyTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.MIDIProgramChangeTriggerTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.MIDISongSelectTriggerTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.PauseTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.RatingTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ScrollBeatModifierTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ScrollBeatTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.SendMIDIClockTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.StartOfHighlightTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.TagTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.TimeTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.TitleTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.VariationsTag
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.song.ScrollingMode

@ParseTags(
	TimeTag::class,
	ImageTag::class,
	MIDISongSelectTriggerTag::class,
	MIDIProgramChangeTriggerTag::class,
	TitleTag::class,
	ArtistTag::class,
	KeyTag::class,
	RatingTag::class,
	PauseTag::class,
	TagTag::class,
	FilterOnlyTag::class,
	BarMarkerTag::class,
	BarsTag::class,
	BeatsPerMinuteTag::class,
	BeatsPerBarTag::class,
	BarsPerLineTag::class,
	ScrollBeatModifierTag::class,
	ScrollBeatTag::class,
	BeatStartTag::class,
	BeatStopTag::class,
	AudioTag::class,
	VariationsTag::class,
	ChordTag::class
)
@IgnoreTags(
	LegacyTag::class, SendMIDIClockTag::class, CommentTag::class, CountTag::class,
	StartOfHighlightTag::class, EndOfHighlightTag::class
)
/**
 * Song file parser. This returns ENOUGH information to display the songs in the song list.
 */
class SongInfoParser(cachedCloudFile: CachedFile) :
	SongFileParser<SongFile>(cachedCloudFile, ScrollingMode.Beat, false, false) {
	private var mTitle: String? = null
	private var mArtist: String? = null
	private var mKey: String? = null
	private var mFirstChord: String? = null
	private var mBPM: Double = 0.0
	private var mBars: Int = 0
	private var mBeats: Int = 0
	private var mTotalPause: Long = 0L
	private var mDuration: Long = 0L
	// Audio files are now a 2D array ... list of audio files per variation.
	private val mAudioFiles = mutableMapOf<String,MutableList<String>>()
	private val mImageFiles = mutableListOf<String>()
	private val mVariations = mutableListOf<String>()
	private var mFilterOnly = false
	private val mTags = mutableListOf<String>()
	private var mMIDIProgramChangeTrigger: SongTrigger? = null
	private var mMIDISongSelectTrigger: SongTrigger? = null
	private var mMixedMode: Boolean = false
	private var mLines = 0
	private var mRating = 0

	override fun parseLine(line: TextFileLine<SongFile>) {
		super.parseLine(line)
		++mLines

		val tagSequence = line.mTags.asSequence()
		val titleTag = tagSequence.filterIsInstance<TitleTag>().firstOrNull()
		val artistTag = tagSequence.filterIsInstance<ArtistTag>().firstOrNull()
		val keyTag = tagSequence.filterIsInstance<KeyTag>().firstOrNull()
		val chordTag = tagSequence.filterIsInstance<ChordTag>().firstOrNull()
		val midiSongSelectTriggerTag =
			tagSequence.filterIsInstance<MIDISongSelectTriggerTag>().firstOrNull()
		val midiProgramChangeTriggerTag =
			tagSequence.filterIsInstance<MIDIProgramChangeTriggerTag>().firstOrNull()
		val bpmTag = tagSequence.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
		val filterOnlyTag = tagSequence.filterIsInstance<FilterOnlyTag>().firstOrNull()
		val beatStartTag = tagSequence.filterIsInstance<BeatStartTag>().firstOrNull()
		val beatStopTag = tagSequence.filterIsInstance<BeatStopTag>().firstOrNull()
		val timeTag = tagSequence.filterIsInstance<TimeTag>().firstOrNull()
		val audioTags = tagSequence.filterIsInstance<AudioTag>()
		val variationsTags = tagSequence.filterIsInstance<VariationsTag>()
		val imageTags = line.mTags.filterIsInstance<ImageTag>()
		val pauseTag = tagSequence.filterIsInstance<PauseTag>().firstOrNull()
		val tagTags = tagSequence.filterIsInstance<TagTag>()
		val ratingTag = tagSequence.filterIsInstance<RatingTag>().firstOrNull()

		if (titleTag != null)
			mTitle = titleTag.mTitle

		if (artistTag != null)
			mArtist = artistTag.mArtist

		if (keyTag != null)
			mKey = keyTag.mKey

		if (filterOnlyTag != null)
			mFilterOnly = true

		if (chordTag != null)
			if (mFirstChord == null && chordTag.mValidChord)
				mFirstChord = chordTag.mName

		if (midiSongSelectTriggerTag != null)
			mMIDISongSelectTrigger = midiSongSelectTriggerTag.mTrigger

		if (midiProgramChangeTriggerTag != null)
			mMIDIProgramChangeTrigger = midiProgramChangeTriggerTag.mTrigger

		if (bpmTag != null)
			mBPM = bpmTag.mBPM

		if (pauseTag != null)
			mTotalPause += pauseTag.mDuration

		if (timeTag != null)
			mDuration = timeTag.mDuration

		if (beatStartTag != null || beatStopTag != null)
			mMixedMode = true

		if (ratingTag != null)
			mRating = ratingTag.mRating

		if (line.mLineWithNoTags.isNotBlank() || imageTags.isNotEmpty() || chordTag != null) {
			mBars += mCurrentLineBeatInfo.mBPL
			mBeats += mCurrentLineBeatInfo.mBeats
		}

		// Variations can only be defined once.
		if (variationsTags.any()) {
			if (mVariations.isEmpty()) {
				mVariations.addAll(variationsTags.flatMap { it.mVariations })
				mVariations.forEach {
					mAudioFiles[it] = mutableListOf()
				}
			} else
				mErrors.add(FileParseError(line.mLineNumber, R.string.variationsAlreadyDefined))
		}

		// Each audio file defined on a line now maps to a variation.
		// The audio filename itself can be a variation name is no explicitly-named variations
		// have been defined. Otherwise, they are mapped to variation by index.
		val noVariationsDefined = mVariations.isEmpty()
		audioTags.forEachIndexed { index, it ->
			val variationName = getVariationNameForIndexedAudioFile(index, line.mLineNumber, it.mFilename, noVariationsDefined)
			if(variationName != null)
				mAudioFiles[variationName]?.add(it.mFilename)
		}
		mImageFiles.addAll(imageTags.map { it.mFilename })
		mTags.addAll(tagTags.map { it.mTag })
	}

	private fun getVariationNameForIndexedAudioFile(index:Int, lineNumber:Int, filename:String, canAddVariation: Boolean):String? {
		if(mVariations.size > index)
			return mVariations[index]
		if(canAddVariation) {
			mVariations.add(filename)
			return filename
		}
		mErrors.add(FileParseError(lineNumber, R.string.tooManyAudioTags))
		return null
	}

	override fun getResult(): SongFile {
		if (mTitle.isNullOrBlank())
			throw InvalidBeatPrompterFileException(R.string.noTitleFound, mCachedCloudFile.mName)
		if (mArtist.isNullOrBlank())
			mArtist = ""
		val key =
			if (mKey.isNullOrBlank())
				if (mFirstChord.isNullOrBlank())
					""
				else
					mFirstChord!!
			else
				mKey!!

		return SongFile(
			mCachedCloudFile,
			mLines,
			mBars,
			mTitle!!,
			mArtist!!,
			key,
			mBPM,
			mDuration,
			mMixedMode,
			mTotalPause,
			mAudioFiles,
			mImageFiles,
			mTags.toSet(),
			mMIDIProgramChangeTrigger
				?: SongTrigger.DEAD_TRIGGER,
			mMIDISongSelectTrigger
				?: SongTrigger.DEAD_TRIGGER,
			mFilterOnly,
			mRating,
			if (mVariations.isEmpty()) listOf("Default") else mVariations,
			mErrors
		)
	}
}
