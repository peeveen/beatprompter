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
import com.stevenfrew.beatprompter.cache.parse.tag.song.MidiProgramChangeTriggerTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.MidiSongSelectTriggerTag
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
import org.w3c.dom.Element

@ParseTags(
	TimeTag::class,
	ImageTag::class,
	MidiSongSelectTriggerTag::class,
	MidiProgramChangeTriggerTag::class,
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
	private var title: String? = null
	private var artist: String? = null
	private var key: String? = null
	private var firstChord: String? = null
	private var bpm: Double = 0.0
	private var bars: Int = 0
	private var beats: Int = 0
	private var totalPauseDuration: Long = 0L
	private var duration: Long = 0L
	private val imageFiles = mutableListOf<String>()
	private var isFilterOnly = false
	private val tags = mutableListOf<String>()
	private var midiProgramChangeTrigger: SongTrigger? = null
	private var midiSongSelectTrigger: SongTrigger? = null
	private var isMixedMode: Boolean = false
	private var lines = 0
	private var rating = 0

	override fun parse(element: Element?): SongFile {
		try {
			SongFile.readSongInfoFromAttributes(element, cachedCloudFile)?.also {
				return it
			}
		} catch (exception: Exception) {
			// Not bothered about what the exception is ... file tags are obviously broken.
			// So re-parse the whole file.
		}
		return super.parse(element)
	}

	override fun parseLine(line: TextFileLine<SongFile>) {
		super.parseLine(line)
		++lines

		val tagSequence = line.tags.asSequence()
		val titleTag = tagSequence.filterIsInstance<TitleTag>().firstOrNull()
		val artistTag = tagSequence.filterIsInstance<ArtistTag>().firstOrNull()
		val keyTag = tagSequence.filterIsInstance<KeyTag>().firstOrNull()
		val chordTag = tagSequence.filterIsInstance<ChordTag>().firstOrNull()
		val midiSongSelectTriggerTag =
			tagSequence.filterIsInstance<MidiSongSelectTriggerTag>().firstOrNull()
		val midiProgramChangeTriggerTag =
			tagSequence.filterIsInstance<MidiProgramChangeTriggerTag>().firstOrNull()
		val bpmTag = tagSequence.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
		val filterOnlyTag = tagSequence.filterIsInstance<FilterOnlyTag>().firstOrNull()
		val beatStartTag = tagSequence.filterIsInstance<BeatStartTag>().firstOrNull()
		val beatStopTag = tagSequence.filterIsInstance<BeatStopTag>().firstOrNull()
		val timeTag = tagSequence.filterIsInstance<TimeTag>().firstOrNull()
		val imageTags = line.tags.filterIsInstance<ImageTag>()
		val pauseTag = tagSequence.filterIsInstance<PauseTag>().firstOrNull()
		val tagTags = tagSequence.filterIsInstance<TagTag>()
		val ratingTag = tagSequence.filterIsInstance<RatingTag>().firstOrNull()

		if (titleTag != null)
			title = titleTag.title

		if (artistTag != null)
			artist = artistTag.artist

		if (keyTag != null)
			key = keyTag.key

		if (filterOnlyTag != null)
			isFilterOnly = true

		if (chordTag != null)
			if (firstChord == null && chordTag.isValidChord)
				firstChord = chordTag.name

		if (midiSongSelectTriggerTag != null)
			midiSongSelectTrigger = midiSongSelectTriggerTag.trigger

		if (midiProgramChangeTriggerTag != null)
			midiProgramChangeTrigger = midiProgramChangeTriggerTag.trigger

		if (bpmTag != null)
			bpm = bpmTag.bpm

		if (pauseTag != null)
			totalPauseDuration += pauseTag.duration

		if (timeTag != null)
			duration = timeTag.duration

		if (beatStartTag != null || beatStopTag != null)
			isMixedMode = true

		if (ratingTag != null)
			rating = ratingTag.rating

		if (line.lineWithNoTags.isNotBlank() || imageTags.isNotEmpty() || chordTag != null) {
			bars += currentLineBeatInfo.mBPL
			beats += currentLineBeatInfo.mBeats
		}

		imageFiles.addAll(imageTags.map { it.filename })
		tags.addAll(tagTags.map { it.tag })
	}

	override fun getResult(): SongFile {
		if (title.isNullOrBlank())
			throw InvalidBeatPrompterFileException(R.string.noTitleFound, cachedCloudFile.name)
		if (artist.isNullOrBlank())
			artist = ""
		val key =
			if (key.isNullOrBlank())
				if (firstChord.isNullOrBlank())
					""
				else
					firstChord!!
			else
				key!!

		return SongFile(
			cachedCloudFile,
			lines,
			bars,
			title!!,
			artist!!,
			key,
			bpm,
			duration,
			isMixedMode,
			totalPauseDuration,
			audioFiles,
			imageFiles,
			tags.toSet(),
			midiProgramChangeTrigger
				?: SongTrigger.DEAD_TRIGGER,
			midiSongSelectTrigger
				?: SongTrigger.DEAD_TRIGGER,
			isFilterOnly,
			rating,
			if (variations.isEmpty()) listOf("Default") else variations,
			errors
		)
	}
}
