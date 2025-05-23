package com.stevenfrew.beatprompter.cache.parse

import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.Handler
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.cache.parse.tag.midi.trigger.MidiProgramChangeTriggerTag
import com.stevenfrew.beatprompter.cache.parse.tag.midi.trigger.MidiSongSelectTriggerTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ActivateMidiAliasesTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ArtistTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.AudioTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BarMarkerTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BarsPerLineTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BarsTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatStartTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatStopTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatsPerBarTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.BeatsPerMinuteTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.CapoTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ChordMapTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ChordTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.CommentTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.CountTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.EndOfChorusTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.EndOfHighlightTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.EndOfVariationExclusionTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.EndOfVariationInclusionTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.FilterOnlyTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.IconTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ImageTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.KeyTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.LegacyTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.MidiEventTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.NoChordsTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.PauseTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.RatingTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ScrollBeatModifierTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ScrollBeatTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.SendMIDIClockTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.StartOfChorusTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.StartOfHighlightTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.StartOfVariationExclusionTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.StartOfVariationInclusionTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.TagTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.TimeTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.TitleTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.TransposeTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.VariationsTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.YearTag
import com.stevenfrew.beatprompter.chord.Chord
import com.stevenfrew.beatprompter.chord.ChordMap
import com.stevenfrew.beatprompter.chord.InvalidChordException
import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.graphics.LineGraphic
import com.stevenfrew.beatprompter.graphics.Rect
import com.stevenfrew.beatprompter.graphics.ScreenString
import com.stevenfrew.beatprompter.midi.BeatBlock
import com.stevenfrew.beatprompter.midi.EventOffsetType
import com.stevenfrew.beatprompter.midi.TriggerOutputContext
import com.stevenfrew.beatprompter.midi.alias.AliasSet
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.Song
import com.stevenfrew.beatprompter.song.event.AudioEvent
import com.stevenfrew.beatprompter.song.event.BaseEvent
import com.stevenfrew.beatprompter.song.event.BeatEvent
import com.stevenfrew.beatprompter.song.event.ClickEvent
import com.stevenfrew.beatprompter.song.event.CommentEvent
import com.stevenfrew.beatprompter.song.event.EndEvent
import com.stevenfrew.beatprompter.song.event.LineEvent
import com.stevenfrew.beatprompter.song.event.LinkedEvent
import com.stevenfrew.beatprompter.song.event.MidiEvent
import com.stevenfrew.beatprompter.song.event.PauseEvent
import com.stevenfrew.beatprompter.song.event.StartEvent
import com.stevenfrew.beatprompter.song.line.ImageLine
import com.stevenfrew.beatprompter.song.line.Line
import com.stevenfrew.beatprompter.song.line.TextLine
import com.stevenfrew.beatprompter.song.load.SongLoadCancelEvent
import com.stevenfrew.beatprompter.song.load.SongLoadCancelledException
import com.stevenfrew.beatprompter.song.load.SongLoadInfo
import com.stevenfrew.beatprompter.ui.BeatCounterTextOverlay
import com.stevenfrew.beatprompter.ui.pref.MetronomeContext
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.getTextOverlayFn
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@ParseTags(
	ImageTag::class,
	PauseTag::class,
	SendMIDIClockTag::class,
	CommentTag::class,
	CountTag::class,
	StartOfHighlightTag::class,
	EndOfHighlightTag::class,
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
	MidiEventTag::class,
	ChordTag::class,
	StartOfVariationExclusionTag::class,
	EndOfVariationExclusionTag::class,
	StartOfVariationInclusionTag::class,
	EndOfVariationInclusionTag::class,
	StartOfChorusTag::class,
	VariationsTag::class,
	EndOfChorusTag::class,
	TransposeTag::class,
	ChordMapTag::class,
	NoChordsTag::class,
	ActivateMidiAliasesTag::class
)
@IgnoreTags(
	LegacyTag::class,
	TimeTag::class,
	MidiSongSelectTriggerTag::class,
	MidiProgramChangeTriggerTag::class,
	TitleTag::class,
	ArtistTag::class,
	KeyTag::class,
	RatingTag::class,
	TagTag::class,
	CapoTag::class,
	YearTag::class,
	IconTag::class,
	FilterOnlyTag::class
)
/**
 * Song file parser. This returns the full information for playing the song.
 */
class SongParser(
	private val songLoadInfo: SongLoadInfo,
	private val supportFileResolver: SupportFileResolver,
	private val songLoadCancelEvent: SongLoadCancelEvent? = null,
	private val songLoadHandler: Handler? = null
) : SongContentParser<Song>(
	songLoadInfo.songInfo.songContentProvider,
	songLoadInfo.initialScrollMode,
	songLoadInfo.mixedModeActive,
	songLoadInfo.variation,
	true
) {
	private val activeMidiAliasSets: MutableSet<AliasSet> =
		Cache.cachedCloudItems.midiAliasSets.filter { it.useByDefault }.toMutableSet()
	private val metronomeContext: MetronomeContext
	private val customCommentsUser: String
	private var showChords: Boolean
	private val showKey: Boolean
	private val showCapo: Boolean
	private val showBpm: ShowBPMContext
	private val triggerContext: TriggerOutputContext
	private val beatCounterTextOverlay: BeatCounterTextOverlay
	private val nativeDeviceSettings: DisplaySettings
	private val initialMidiMessages =
		Cache.cachedCloudItems.initialMidiMessages.toMutableList()
	private var stopAddingStartupItems = false
	private val startScreenComments = mutableListOf<Song.Comment>()
	private val events = mutableListOf<BaseEvent>()
	private val lines = LineList()
	private val rolloverBeats = mutableListOf<BeatEvent>()
	private val beatBlocks = mutableListOf<BeatBlock>()
	private val paint = Paint()
	private val defaultHighlightColor: Int
	private val timePerBar: Long
	private val flatAudioFiles: List<AudioFile>

	private var songHeight = 0
	private var midiBeatCounter: Int = 0
	private var lastBeatBlock: BeatBlock? = null
	private var beatsToAdjust: Int = 0
	private var currentBeat: Int = 0
	private var countIn: Int
	private var sendMidiClock = false
	private var songTime: Long = 0
	private var defaultMidiOutputChannel: Byte
	private var isInChorusSection = false
	private var pendingAudioTag: AudioTag? = null
	private var audioTagIndex: Int = 0
	private var chordMap: ChordMap? = songLoadInfo.songInfo.firstChord?.let {
		ChordMap(
			songLoadInfo.songInfo.chords.toSet(),
			it,
			songLoadInfo.songInfo.keySignature
		).transpose(songLoadInfo.transposeShift)
	}
	private var firstParsedChord: String? = null

	init {
		// All songFile info parsing errors count as our errors too.
		songLoadInfo.songInfo.errors.forEach { addError(it) }

		sendMidiClock = BeatPrompter.preferences.sendMIDIClock
		countIn = BeatPrompter.preferences.defaultCountIn
		beatCounterTextOverlay = BeatPrompter.preferences.beatCounterTextOverlay
		metronomeContext = BeatPrompter.preferences.metronomeContext
		defaultHighlightColor = BeatPrompter.preferences.defaultHighlightColor
		customCommentsUser = BeatPrompter.preferences.customCommentsUser
		showChords = BeatPrompter.preferences.showChords
		triggerContext = BeatPrompter.preferences.sendMIDITriggerOnStart
		val defaultMIDIOutputChannelPrefValue = BeatPrompter.preferences.defaultMIDIOutputChannel
		defaultMidiOutputChannel = MidiMessage.getChannelFromBitmask(defaultMIDIOutputChannelPrefValue)
		showKey =
			BeatPrompter.preferences.showKey && !songLoadInfo.songInfo.keySignature.isNullOrBlank()
		showCapo =
			BeatPrompter.preferences.showCapo && songLoadInfo.songInfo.capo != 0
		showBpm =
			if (songLoadInfo.songInfo.bpm > 0.0) BeatPrompter.preferences.showBPMContext else ShowBPMContext.No

		// Figure out the screen size
		nativeDeviceSettings = translateSourceDeviceSettingsToNative(
			songLoadInfo.sourceDisplaySettings,
			songLoadInfo.nativeDisplaySettings
		)

		// Start the progress message dialog
		songLoadHandler?.obtainMessage(
			Events.SONG_LOAD_LINE_PROCESSED,
			0, songLoadInfo.songInfo.lines
		)?.sendToTarget()

		val selectedVariation = songLoadInfo.variation
		val audioFilenamesForThisVariation =
			songLoadInfo.songInfo.audioFiles[selectedVariation] ?: listOf()
		flatAudioFiles = audioFilenamesForThisVariation.mapNotNull {
			supportFileResolver.getMappedAudioFiles(it).firstOrNull()
		}
		val lengthOfBackingTrack = flatAudioFiles.firstOrNull()?.duration ?: 0L
		var songTime =
			if (songLoadInfo.songInfo.duration == Utils.milliToNano(Utils.TRACK_AUDIO_LENGTH_VALUE))
				lengthOfBackingTrack
			else
				songLoadInfo.songInfo.duration
		if (songTime > 0 && songLoadInfo.songInfo.totalPauseDuration > songTime) {
			addError(ContentParsingError(R.string.pauseLongerThanSong))
			ongoingBeatInfo = SongBeatInfo(scrollMode = ScrollingMode.Manual)
			currentLineBeatInfo = LineBeatInfo(ongoingBeatInfo)
			songTime = 0
		}

		timePerBar =
			if (songTime > 0L)
				(songTime.toDouble() / songLoadInfo.songInfo.bars).toLong()
			else
				0
	}

	private fun getLineChordMap(chordTags: List<ChordTag>): ChordMap {
		firstParsedChord = firstParsedChord ?: chordTags.firstNotNullOfOrNull {
			try {
				Chord.parse(it.name)
			} catch (_: InvalidChordException) {
				// Oh well!
				null
			}
		}?.toDisplayString()
		return ChordMap(
			chordTags.map { it.name }.toSet(),
			songLoadInfo.songInfo.firstChord ?: firstParsedChord ?: "C",
			songLoadInfo.songInfo.keySignature
		)
	}

	override fun parseLine(line: TextContentLine<Song>): Boolean {
		if (songLoadCancelEvent?.isCancelled == true)
			throw SongLoadCancelledException()
		if (!super.parseLine(line))
			return false

		showChords = showChords and !line.tags.filterIsInstance<NoChordsTag>().any()
		val chordTags = line.tags.filterIsInstance<ChordTag>()
		val nonChordTags = line.tags.filter { it !is ChordTag }
		var lineChordMap = chordMap ?: getLineChordMap(chordTags).transpose(songLoadInfo.transposeShift)
		val chordsFound = chordTags.isNotEmpty()
		val chordsFoundButNotShowingThem = !showChords && chordsFound
		val tags = if (showChords) line.tags.toList() else nonChordTags
		val tagSequence = tags.asSequence()

		val transposeTags = tagSequence.filterIsInstance<TransposeTag>()
		transposeTags.forEach {
			try {
				lineChordMap = lineChordMap.transpose(it.value)
				if (chordMap != null)
					chordMap = lineChordMap
			} catch (e: Exception) {
				addError(ContentParsingError(it, e))
			}
		}

		activeMidiAliasSets.addAll(
			tagSequence.filterIsInstance<ActivateMidiAliasesTag>().flatMap { it.midiAliasSets })

		val chordMapTags = tagSequence.filterIsInstance<ChordMapTag>()
		chordMapTags.forEach {
			lineChordMap = lineChordMap.addChordMapping(it.from, it.to)
			if (chordMap != null)
				chordMap = lineChordMap
		}

		var workLine = line.lineWithNoTags

		// Generate clicking beats if the metronome is on.
		// The "on when no track" logic will be performed during song playback.
		val metronomeOn =
			metronomeContext === MetronomeContext.On || metronomeContext === MetronomeContext.OnWhenNoTrack

		var imageTag = tagSequence.filterIsInstance<ImageTag>().firstOrNull()

		val startOfChorusTag = tagSequence.filterIsInstance<StartOfChorusTag>().firstOrNull()
		val thisLineIsInChorus = startOfChorusTag != null || isInChorusSection
		val endOfChorusTag = tagSequence.filterIsInstance<EndOfChorusTag>().firstOrNull()
		isInChorusSection =
			if (endOfChorusTag != null) {
				if (startOfChorusTag != null)
					endOfChorusTag.position < startOfChorusTag.position
				else
					false
			} else
				startOfChorusTag != null || isInChorusSection

		if (!sendMidiClock)
			sendMidiClock = tags.any { it is SendMIDIClockTag }

		if (!stopAddingStartupItems)
			countIn = tags.filterIsInstance<CountTag>().firstOrNull()?.count ?: countIn

		tags
			.filterIsInstance<CommentTag>()
			.map {
				Song.Comment(
					it.comment,
					it.audience,
					it.color
						?: if (stopAddingStartupItems) BeatPrompter.preferences.commentColor else DEFAULT_START_SCREEN_COMMENT_COLOR,
					Rect(nativeDeviceSettings.screenSize),
					paint
				)
			}
			.filter { it.isIntendedFor(customCommentsUser) }
			.forEach {
				if (stopAddingStartupItems)
					events.add(CommentEvent(songTime, it))
				else
					startScreenComments.add(it)
			}

		// If a line has a number of bars defined, we really should treat it as a line, even if
		// is blank.
		val shorthandBarTag = tagSequence
			.filterIsInstance<BarMarkerTag>()
			.firstOrNull()
		val barsTag = tagSequence
			.filterIsInstance<BarsTag>()
			.firstOrNull()
		// Contains only tags? Or contains nothing? Don't use it as a blank line.
		// BUT! If there are bar indicators of any kind, use the blank line.
		val isLineContent = (workLine.isNotEmpty()
			|| chordsFoundButNotShowingThem
			|| chordsFound
			|| imageTag != null
			|| shorthandBarTag != null
			|| (barsTag != null && barsTag.bars > 0))

		val pauseTag = tagSequence
			.filterIsInstance<PauseTag>()
			.firstOrNull()

		val isLineContentOrPause = isLineContent || pauseTag != null

		tags
			.filterIsInstance<MidiEventTag>()
			// Only use MIDI events generated from alias sets that are ALL active.
			// We could have a recursive chain of aliases between sets, but if even
			// one set is inactive, the whole thing is invalid.
			.filter {
				activeMidiAliasSets.containsAll(it.aliasSetsUsed)
			}
			.forEach {
				if (stopAddingStartupItems || isLineContentOrPause)
					events.add(it.toMIDIEvent(songTime))
				else {
					initialMidiMessages.addAll(it.messages)
					if (it.offset.amount != 0)
						addError(ContentParsingError(it, R.string.midi_offset_before_first_line))
				}
			}

		// An audio tag is only processed when there is actual line content.
		// But the audio tag can be defined on a line without content.
		// In which case it is "pending", to be applied when line content is found.
		pendingAudioTag =
			if (tags.filterIsInstance<AudioTag>().any() && !songLoadInfo.noAudio) getVariationAudioTag(
				audioTagIndex++
			) else pendingAudioTag

		if (isLineContentOrPause) {
			// We definitely have a line!
			// So now is when we want to create the count-in (if any)
			if (countIn > 0) {
				val countInEvents = generateCountInEvents(
					countIn,
					metronomeContext === MetronomeContext.DuringCountIn || metronomeOn
				)
				events.addAll(countInEvents.mEvents)
				songTime = countInEvents.mBlockEndTime
				countIn = 0
			}

			// If there is a beatstart, we add a StartEvent. This functions as a "current event" that
			// the song can be set to, then advanced from. The "current event" is not processed when we
			// "press play" to start the song (it is expected that it ALREADY has been processed).
			// StartEvents function as simply dummy starting-point "current" events.
			val beatStartTag = tagSequence.filterIsInstance<BeatStartTag>().firstOrNull()
			if (beatStartTag != null)
				events.add(StartEvent(songTime))

			pendingAudioTag?.also {
				// Make sure file exists.
				val mappedTracks =
					supportFileResolver.getMappedAudioFiles(it.normalizedFilename)
				if (mappedTracks.isEmpty())
					addError(ContentParsingError(it, R.string.cannotFindAudioFile, it.normalizedFilename))
				else if (mappedTracks.size > 1)
					addError(
						ContentParsingError(
							it,
							R.string.multipleFilenameMatches,
							it.normalizedFilename
						)
					)
				else {
					val audioFile = mappedTracks.first()
					if (!audioFile.file.exists())
						addError(
							ContentParsingError(
								it,
								R.string.cannotFindAudioFile,
								it.normalizedFilename
							)
						)
					else
						events.add(
							AudioEvent(
								songTime,
								audioFile,
								it.volume,
								!stopAddingStartupItems
							)
						)
				}
			}
			// Clear the pending tag.
			pendingAudioTag = null

			// Any comments or MIDI events from here will be part of the song,
			// rather than startup events.
			stopAddingStartupItems = true

			if (imageTag != null && (workLine.isNotBlank() || chordsFound))
				addError(ContentParsingError(line.lineNumber, R.string.text_found_with_image))

			// Measuring a blank line will result in a 0x0 measurement, so we
			// need to have SOMETHING to measure. A nice wee "down arrow" should look OK.
			if (workLine.isBlank() && (!chordsFound || chordsFoundButNotShowingThem))
				workLine = "▼"

			// Generate pause events if required (may return null)
			val pauseEvents = generatePauseEvents(songTime, pauseTag)
			val paused = pauseEvents?.any() == true
			if (paused || currentLineBeatInfo.scrollMode !== ScrollingMode.Beat)
				rolloverBeats.clear()

			if (isLineContent) {
				// First line should always have a time of zero, so that if the user scrolls
				// back to the start of the song, it still picks up any count-in beat events.
				val lineStartTime = if (lines.isEmpty()) 0L else songTime

				// If the first line is a pause event, we need to adjust the total line time accordingly
				// to include any count-in
				val addToPause = if (lines.isEmpty()) songTime else 0L

				// Generate beat events (may return null in smooth mode)
				pauseEvents?.maxOf { it.eventTime }
				val beatEvents = if (paused || currentLineBeatInfo.scrollMode === ScrollingMode.Smooth)
					EventBlock(listOf(), pauseEvents?.maxOf { it.eventTime } ?: 0)
				else
					generateBeatEvents(songTime, metronomeOn)

				// Calculate how long this line will last for
				val lineDuration = calculateLineDuration(
					pauseTag,
					addToPause,
					lineStartTime,
					beatEvents
				)

				// Calculate the start and stop scroll times for this line
				val startAndStopScrollTimes = calculateStartAndStopScrollTimes(
					pauseTag,
					lineStartTime + addToPause,
					lineDuration,
					beatEvents,
					songLoadInfo.audioLatency
				)

				// Create the line
				var lineObj: Line? = null
				if (imageTag != null) {
					val imageFiles =
						supportFileResolver.getMappedImageFiles(imageTag.filename)
					if (imageFiles.isNotEmpty())
						try {
							lineObj = ImageLine(
								imageFiles.first(),
								imageTag.scalingMode,
								lineStartTime,
								lineDuration,
								currentLineBeatInfo.scrollMode,
								nativeDeviceSettings,
								songHeight,
								thisLineIsInChorus,
								startAndStopScrollTimes
							)
						} catch (t: Throwable) {
							// Bitmap loading could cause error here. Even OutOfMemory!
							addError(ContentParsingError(imageTag, t))
						}
					else {
						workLine = BeatPrompter.appResources.getString(R.string.missing_image_file_warning)
						addError(ContentParsingError(imageTag, R.string.missing_image_file_warning))
						imageTag = null
					}
				}
				if (imageTag == null)
					lineObj = TextLine(
						workLine,
						tags,
						lineStartTime,
						lineDuration,
						currentLineBeatInfo.scrollMode,
						nativeDeviceSettings,
						lines
							.filterIsInstance<TextLine>()
							.lastOrNull()?.trailingHighlightColor,
						songHeight,
						thisLineIsInChorus,
						startAndStopScrollTimes,
						lineChordMap,
						songLoadCancelEvent
					)

				if (lineObj != null) {
					lines.add(lineObj)
					events.add(LineEvent(lineObj.lineTime, lineObj))

					songHeight += lineObj.measurements.lineHeight

					// If a pause is going to be generated, then we don't need beats.
					if (pauseEvents == null) {
						// Otherwise, add any generated beats
						if (beatEvents?.mEvents?.isNotEmpty() == true) {
							events.addAll(beatEvents.mEvents)
							songTime = beatEvents.mBlockEndTime
						}
						// Otherwise, forget it, just bump up the song time
						else
							songTime += lineDuration
					}
				}
			}
			// Now add the pause events to the song (if required).
			if (pauseEvents != null && pauseTag != null) {
				events.addAll(pauseEvents)
				songTime += pauseTag.duration
			}
		}
		if (!isLineContent || currentLineBeatInfo.scrollMode !== ScrollingMode.Beat)
		// If there is no actual line data (or if the line is a manual mode line), then the scroll beat offset never took effect.
		// Clear it so that the next line (which MIGHT be a proper line) doesn't take it into account.
			currentLineBeatInfo = LineBeatInfo(
				currentLineBeatInfo.beats,
				currentLineBeatInfo.bpl,
				currentLineBeatInfo.bpb,
				currentLineBeatInfo.bpm,
				currentLineBeatInfo.scrollBeat,
				currentLineBeatInfo.lastScrollBeatTotalOffset,
				0, currentLineBeatInfo.scrollMode
			)

		songLoadHandler?.obtainMessage(
			Events.SONG_LOAD_LINE_PROCESSED,
			line.lineNumber, songLoadInfo.songInfo.lines
		)?.sendToTarget()
		return true
	}

	private fun getVariationAudioTag(index: Int): AudioTag? =
		variationAudioTags[variation]?.let {
			if (it.count() > index)
				it[index]
			else
				null
		}

	override fun getResult(): Song {
		// Song has no lines? Make a dummy line so we don't have to check for null everywhere in the code.
		if (lines.isEmpty())
			throw InvalidBeatPrompterFileException(R.string.no_lines_in_song_file)

		val lineSequence = lines.asSequence()
		val smoothMode = lineSequence.filter { it.scrollMode == ScrollingMode.Smooth }.any()

		val startScreenStrings = createStartScreenStrings()
		val totalStartScreenTextHeight = startScreenStrings.first.sumOf { it.height }

		// In smooth scrolling mode, the display will start scrolling immediately.
		// This is an essential feature of smooth scrolling mode, yet causes a problem: the first line
		// will almost immediately become obscured, just as you are performing it.
		// To combat this, there will an initial blank "buffer zone", created by offsetting the graphical
		// display by a number of pixels.
		val smoothScrollOffset =
			if (smoothMode)
			// Obviously this will only be required if the song cannot fit entirely onscreen.
				if (songHeight > nativeDeviceSettings.usableScreenHeight)
					min(lineSequence.map { it.measurements.lineHeight }.maxByOrNull { it }
						?: 0, (nativeDeviceSettings.screenSize.height / 3.0).toInt())
				else
					0
			else
				0

		// Get all required audio info ...
		val audioEvents = events.filterIsInstance<AudioEvent>()

		// Allocate graphics objects.
		val maxGraphicsRequired = getMaximumGraphicsRequired(nativeDeviceSettings.screenSize.height)
		val lineGraphics = CircularGraphicsList()
		repeat(maxGraphicsRequired) {
			lineGraphics.add(LineGraphic(getBiggestLineSize(it, maxGraphicsRequired)))
		}

		// There may be no lines! So we have to check ...
		if (lineGraphics.isNotEmpty()) {
			var graphic: LineGraphic = lineGraphics.first()
			lines.forEach { line ->
				repeat(line.measurements.lines) {
					line.allocateGraphic(graphic)
					graphic = graphic.nextGraphic
				}
			}
		}

		val beatCounterHeight = nativeDeviceSettings.beatCounterRect.height
		val maxSongTitleWidth = nativeDeviceSettings.screenSize.width * 0.9f
		val maxSongTitleHeight = beatCounterHeight * 0.9f
		val vMargin = (beatCounterHeight - maxSongTitleHeight) / 2.0f
		val beatCounterTextOverlayScreenString = ScreenString.create(
			beatCounterTextOverlay.getTextOverlayFn(songLoadInfo.songInfo.title),
			paint,
			maxSongTitleWidth.toInt(),
			maxSongTitleHeight.toInt(),
			Utils.makeHighlightColour(Color.BLACK, 0x80.toByte()),
			false
		)
		val extraMargin = (maxSongTitleHeight - beatCounterTextOverlayScreenString.height) / 2.0f
		val x =
			((nativeDeviceSettings.screenSize.width - beatCounterTextOverlayScreenString.width) / 2.0).toFloat()
		val y =
			beatCounterHeight - (extraMargin + beatCounterTextOverlayScreenString.descenderOffset.toFloat() + vMargin)
		val songTitleHeaderLocation = PointF(x, y)

		// First of all, find beat events that have the "click" flag set and
		// add a click event (necessary because we want to offset the click by
		// the audio latency without offsetting the beat).
		val eventsWithClicks = generateClickEvents(events)
		// Now offset any MIDI events that have an offset.
		val midiOffsetEventList = offsetMIDIEvents(eventsWithClicks)
		// And offset non-audio events by the audio latency offset.
		val audioLatencyCompensatedEventList =
			compensateForAudioLatency(midiOffsetEventList, Utils.milliToNano(songLoadInfo.audioLatency))

		// OK, now sort all events by time, and type within time
		val sortedEventList = sortEvents(audioLatencyCompensatedEventList).toMutableList()

		// Songs need a "first event" to have as their "current event". Without this, the initial
		// "current event" could be the EndEvent!
		sortedEventList.add(0, StartEvent())

		// Now we need to figure out which lines should NOT scroll offscreen.
		val noScrollLines = mutableListOf<Line>()
		val lastLineIsBeat = lines.lastOrNull()?.scrollMode == ScrollingMode.Beat
		if (lastLineIsBeat) {
			noScrollLines.add(lines.last())
			// Why was I removing this? It breaks highlighting the last line ...
			// sortedEventList.removeAt(sortedEventList.indexOfLast { it is LineEvent })
		} else if (smoothMode) {
			var availableScreenHeight = nativeDeviceSettings.usableScreenHeight - smoothScrollOffset
			val lineEvents = sortedEventList.filterIsInstance<LineEvent>()
			for (lineEvent in lineEvents.reversed()) {
				availableScreenHeight -= lineEvent.line.measurements.lineHeight
				if (availableScreenHeight >= 0) {
					noScrollLines.add(lineEvent.line)
					sortedEventList.remove(lineEvent)
				} else
					break
			}
		}

		// To generate the EndEvent, we need to know the time that the
		// song ends. This could be the time of the final generated event,
		// but there might still be an audio file playing, so find out
		// when the last track ends ...
		val lastAudioEndTime = sortedEventList
			.asSequence()
			.filterIsInstance<AudioEvent>()
			.map { it.audioFile.duration + it.eventTime }
			.maxOrNull()
		sortedEventList.add(EndEvent(max(lastAudioEndTime ?: 0L, songTime)))

		// Now build the final event list.
		val firstEvent = LinkedEvent(sortedEventList)

		// Calculate the last position that we can scroll to.
		val scrollEndPixel = calculateScrollEndPixel(smoothMode, smoothScrollOffset)

		if ((triggerContext == TriggerOutputContext.Always)
			|| (triggerContext == TriggerOutputContext.ManualStartOnly && !songLoadInfo.wasStartedByMidiTrigger)
		) {
			initialMidiMessages.addAll(
				songLoadInfo.songInfo.programChangeTrigger.getMIDIMessages(
					defaultMidiOutputChannel
				)
			)
			initialMidiMessages.addAll(
				songLoadInfo.songInfo.songSelectTrigger.getMIDIMessages(
					defaultMidiOutputChannel
				)
			)
		}

		return Song(
			songLoadInfo.songInfo,
			nativeDeviceSettings,
			firstEvent,
			lines,
			audioEvents,
			initialMidiMessages,
			beatBlocks,
			sendMidiClock,
			startScreenStrings.first,
			startScreenStrings.second,
			totalStartScreenTextHeight,
			songLoadInfo.wasStartedByBandLeader,
			songLoadInfo.nextSong,
			smoothScrollOffset,
			songHeight,
			scrollEndPixel,
			noScrollLines,
			nativeDeviceSettings.beatCounterRect,
			beatCounterTextOverlayScreenString,
			songTitleHeaderLocation,
			songLoadInfo.loadId,
			songLoadInfo.audioLatency,
			activeMidiAliasSets
		)
	}

	private fun calculateScrollEndPixel(smoothMode: Boolean, smoothScrollOffset: Int): Int {
		val manualDisplayEnd = max(0, songHeight - nativeDeviceSettings.usableScreenHeight)
		val beatDisplayEnd =
			lines.lastOrNull { it.scrollMode === ScrollingMode.Beat }?.songPixelPosition
		return if (smoothMode)
			manualDisplayEnd + smoothScrollOffset//+smoothScrollEndOffset
		else if (beatDisplayEnd != null)
			if (beatDisplayEnd + nativeDeviceSettings.usableScreenHeight > songHeight)
				beatDisplayEnd
			else
				manualDisplayEnd
		else
			manualDisplayEnd
	}

	private fun getBiggestLineSize(index: Int, modulus: Int): Rect {
		var maxHeight = 0
		var maxWidth = 0
		var lineCount = 0
		lines.forEach {
			for (lh in it.measurements.graphicHeights) {
				if (lineCount % modulus == index) {
					maxHeight = max(maxHeight, lh)
					maxWidth = max(maxWidth, it.measurements.lineWidth)
				}
				++lineCount
			}
		}
		return Rect(0, 0, maxWidth - 1, maxHeight - 1)
	}

	private fun getMaximumGraphicsRequired(screenHeight: Int): Int =
		lines.indices.maxOfOrNull {
			// acc is a Pair of heightCounter and lineCounter
			lines.subList(it, lines.size).fold(0 to 0) { acc, line ->
				val (heightCounter, lineCounter) = acc
				if (heightCounter < screenHeight) {
					// Assume height of first line to be 1 pixel
					// This is the state of affairs when the top line is almost
					// scrolled offscreen, but not quite.
					(heightCounter + if (lineCounter > 0) line.measurements.lineHeight else 1) to (lineCounter + line.measurements.lines)
				} else acc
			}.second
		} ?: 0

	private fun generateBeatEvents(startTime: Long, click: Boolean): EventBlock? {
		if (currentLineBeatInfo.scrollMode === ScrollingMode.Smooth)
			return null
		var eventTime = startTime
		val beatEvents = mutableListOf<BeatEvent>()
		var beatThatWeWillScrollOn = 0
		val currentTimePerBeat = Utils.nanosecondsPerBeat(currentLineBeatInfo.bpm)
		// If the previous line scrolled early, there will still be some beats left over,
		// so they need to be added to this beat event group.
		val rolloverBeatCount = rolloverBeats.size
		var rolloverBeatsApplied = 0
		// We have N beats to adjust, because the BPB has changed on this line, and the
		// previous line scrolled late, meaning that one or more beat events from the previous
		// beat-event group will have incorrect BPB values.
		// If we don't adjust the BPB of the previous beats to the BPB from THIS line, they
		// will not appear correctly.
		// So ... for the previous N beat events, set the BPB to the new BPB.
		if (beatsToAdjust > 0) {
			events.filterIsInstance<BeatEvent>().takeLast(beatsToAdjust).forEach {
				it.bpb = currentLineBeatInfo.bpb
			}
			beatsToAdjust = 0
		}

		// Okay, we can now create the beat events for this line.
		(0 until currentLineBeatInfo.beats).forEach { beatIndex ->
			currentLineBeatInfo.beats - beatIndex
			val beatsRemaining = currentLineBeatInfo.beats - beatIndex
			// Calculate the scroll beat.
			// If the number of beats remaining in this line is greater than the BPB,
			// then the scroll beat won't be happening in the bar that this beat is in.
			beatThatWeWillScrollOn = if (beatsRemaining > currentLineBeatInfo.bpb) -1 else
			// Otherwise the scrollbeat will be coming up ...
				((currentLineBeatInfo.bpb * 2 + currentLineBeatInfo.scrollBeatTotalOffset - 1) % currentLineBeatInfo.bpb).let {
					// If the scrollbeat is the current beat, we don't want to display the marker on this beat.
					if (it == currentBeat)
						-2
					else
						it
				}
			// If we have a non-negative value for this beatThatWeWillScrollOn, we can retroactively
			// set beat events with this to improve visibility.

			if (beatThatWeWillScrollOn >= 0) {
				// Find the previous beat event, with a beat counter at least equal to the scrollbeat,
				// with a -2 value for willScrollOnBeat
				(if (beatEvents.isEmpty()) events.filterIsInstance<BeatEvent>() else beatEvents)
					.takeLastWhile { it.willScrollOnBeat == -2 }
					.forEach { it.willScrollOnBeat = beatThatWeWillScrollOn }
			}
			var rolloverBPB = 0
			var rolloverBeatLength = 0L
			// Create the next beat event. If there are rollover beats (caused by the
			// previous line scrolling early), "use them up" first before creating
			// new beat events for this line.
			val beatEvent = if (rolloverBeats.isEmpty())
				BeatEvent(
					eventTime,
					currentLineBeatInfo.bpm,
					currentLineBeatInfo.bpb,
					currentBeat,
					click,
					beatThatWeWillScrollOn
				)
			else {
				// Take a rollover beat from the pile.
				val rolloverBeatEvent = rolloverBeats.removeAt(0)
				// Update it with the new beatThatWeWillScrollOn value.
				val modifiedRolloverBeatEvent = BeatEvent(
					rolloverBeatEvent.eventTime,
					rolloverBeatEvent.bpm,
					rolloverBeatEvent.bpb,
					rolloverBeatEvent.beat,
					rolloverBeatEvent.click,
					beatThatWeWillScrollOn
				)
				rolloverBPB = modifiedRolloverBeatEvent.bpb
				rolloverBeatLength = Utils.nanosecondsPerBeat(rolloverBeatEvent.bpm)
				rolloverBeatsApplied += 1
				modifiedRolloverBeatEvent
			}
			beatEvents.add(beatEvent)
			val beatTimeLength = if (rolloverBeatLength == 0L) currentTimePerBeat else rolloverBeatLength

			// generate MIDI beat blocks.
			val nanoPerBeat = beatTimeLength / 4.0
			if (lastBeatBlock == null || nanoPerBeat != lastBeatBlock!!.nanoPerBeat) {
				lastBeatBlock = BeatBlock(beatEvent.eventTime, midiBeatCounter++, nanoPerBeat)
				beatBlocks.add(lastBeatBlock!!)
			}

			eventTime += beatTimeLength

			// Keep track of the current beat number.
			currentBeat++
			if (currentBeat == (if (rolloverBPB > 0) rolloverBPB else currentLineBeatInfo.bpb))
				currentBeat = 0
		}

		val beatsThisLine = currentLineBeatInfo.beats - rolloverBeatCount + rolloverBeatsApplied
		val simpleBeatsThisLine =
			(currentLineBeatInfo.bpb * currentLineBeatInfo.bpl) - currentLineBeatInfo.lastScrollBeatTotalOffset
		if (beatsThisLine > simpleBeatsThisLine) {
			// This line is scrolling EARLY.
			// We need to store some information so that the next line can adjust the rollover beats.
			beatsToAdjust = currentLineBeatInfo.beats - simpleBeatsThisLine
		} else if (beatsThisLine < simpleBeatsThisLine) {
			// This line is scrolling LATE.
			// We need to generate a few beats to store for the next line to use.
			rolloverBeats.clear()
			var rolloverCurrentBeat = currentBeat
			var rolloverCurrentTime = eventTime
			repeat(simpleBeatsThisLine - beatsThisLine) {
				rolloverBeats.add(
					BeatEvent(
						rolloverCurrentTime,
						currentLineBeatInfo.bpm,
						currentLineBeatInfo.bpb,
						rolloverCurrentBeat++,
						click,
						beatThatWeWillScrollOn
					)
				)
				rolloverCurrentTime += currentTimePerBeat
				if (rolloverCurrentBeat == currentLineBeatInfo.bpb)
					rolloverCurrentBeat = 0
			}
		}
		return EventBlock(beatEvents, eventTime)
	}

	private fun generatePauseEvents(startTime: Long, pauseTag: PauseTag?): List<PauseEvent>? {
		if (pauseTag == null)
			return null
		// pauseTime is in milliseconds.
		// We don't want to generate thousands of events, so let's say every 1/10th of a second.
		// Or, if the pause is pretty big, 1% of the pause length. Whichever is bigger.
		var eventTime = startTime
		val onePercentOfPause = (pauseTag.duration / 100.0).toLong()
		val pauseEvents = mutableListOf<PauseEvent>()
		val pauseEventGranularity = max(onePercentOfPause, ONE_DECISECOND_IN_NANOSECONDS)
		val events =
			ceil(pauseTag.duration.toDouble() / pauseEventGranularity).toInt()
		val remainder = pauseTag.duration % pauseEventGranularity
		eventTime += remainder
		repeat(events) {
			val pauseEvent = PauseEvent(eventTime, events, it)
			pauseEvents.add(pauseEvent)
			eventTime += pauseEventGranularity
		}
		return pauseEvents
	}

	private fun generateCountInEvents(countBars: Int, click: Boolean): EventBlock {
		val countInEvents = mutableListOf<BeatEvent>()
		var startTime = 0L
		if (countBars > 0) {
			if (currentLineBeatInfo.bpm > 0.0) {
				val countBpm = currentLineBeatInfo.bpm
				val countBpb = currentLineBeatInfo.bpb
				val nanoPerBeat = Utils.nanosecondsPerBeat(countBpm)
				repeat(countBars) { bar ->
					repeat(countBpb) { beat ->
						countInEvents.add(
							BeatEvent(
								startTime,
								currentLineBeatInfo.bpm,
								currentLineBeatInfo.bpb,
								beat,
								click,
								if (bar == countBars - 1) countBpb - 1 else -1
							)
						)
						startTime += nanoPerBeat
					}
				}
			}
		}
		return EventBlock(countInEvents, startTime)
	}

	/**
	 * Based on the difference in screen size/resolution/orientation, we will alter the min/max font size of our native settings.
	 */
	private fun translateSourceDeviceSettingsToNative(
		sourceSettings: DisplaySettings,
		nativeSettings: DisplaySettings
	): DisplaySettings {
		val sourceScreenSize = sourceSettings.screenSize
		val sourceRatio = sourceScreenSize.width.toDouble() / sourceScreenSize.height.toDouble()
		val screenWillRotate = nativeSettings.orientation != sourceSettings.orientation
		val nativeScreenSize = if (screenWillRotate)
			Rect(0, 0, nativeSettings.screenSize.height, nativeSettings.screenSize.width)
		else
			nativeSettings.screenSize
		val nativeRatio = nativeScreenSize.width.toDouble() / nativeScreenSize.height.toDouble()
		val minRatio = min(nativeRatio, sourceRatio)
		val maxRatio = max(nativeRatio, sourceRatio)
		val ratioMultiplier = minRatio / maxRatio
		var minimumFontSize = sourceSettings.minimumFontSize
		var maximumFontSize = sourceSettings.maximumFontSize
		minimumFontSize *= ratioMultiplier.toFloat()
		maximumFontSize *= ratioMultiplier.toFloat()
		if (minimumFontSize > maximumFontSize) {
			addError(ContentParsingError(0, R.string.fontSizesAllMessedUp))
			maximumFontSize = minimumFontSize
		}
		return DisplaySettings(
			sourceSettings.orientation,
			minimumFontSize,
			maximumFontSize,
			nativeScreenSize,
			sourceSettings.showBeatCounter
		)
	}

	private fun createStartScreenStrings(): Pair<List<ScreenString>, ScreenString?> {
		// As for the start screen display (title/artist/comments/"press go"),
		// the title should take up no more than 20% of the height, the artist
		// no more than 10%, also 10% for the "press go" message.
		// The rest of the space is allocated for the comments and error messages,
		// each line no more than 10% of the screen height.
		val startScreenStrings = mutableListOf<ScreenString>()
		var availableScreenHeight = nativeDeviceSettings.screenSize.height
		var nextSongString: ScreenString? = null
		if (songLoadInfo.nextSong.isNotBlank()) {
			// OK, we have a next song title to display.
			// This should take up no more than 15% of the screen.
			// But that includes a border, so use 13 percent for the text.
			val eightPercent = (nativeDeviceSettings.screenSize.height * 0.13).toInt()
			val nextSong = songLoadInfo.nextSong
			val fullString = ">>> $nextSong >>>"
			nextSongString = ScreenString.create(
				fullString,
				paint,
				nativeDeviceSettings.screenSize.width,
				eightPercent,
				Color.BLACK,
				true
			)
			availableScreenHeight -= (nativeDeviceSettings.screenSize.height * 0.15f).toInt()
		}
		val tenPercent = (availableScreenHeight / 10.0).toInt()
		val twentyPercent = (availableScreenHeight / 5.0).toInt()
		startScreenStrings.add(
			ScreenString.create(
				songLoadInfo.songInfo.title,
				paint,
				nativeDeviceSettings.screenSize.width,
				twentyPercent,
				START_SCREEN_TITLE_COLOR,
				true
			)
		)
		if (songLoadInfo.songInfo.artist.isNotBlank())
			startScreenStrings.add(
				ScreenString.create(
					songLoadInfo.songInfo.artist,
					paint,
					nativeDeviceSettings.screenSize.width,
					tenPercent,
					START_SCREEN_ARTIST_COLOR,
					true
				)
			)
		val nonBlankCommentLines = startScreenComments.filter { it.text.trim().isNotEmpty() }
		val uniqueErrors = errors.asSequence().distinct().sortedBy { it.lineNumber }.toList()
		var errorCount = uniqueErrors.size
		var messages = min(errorCount, 6) + nonBlankCommentLines.size
		val showBPM = showBpm != ShowBPMContext.No
		if (showBPM)
			++messages
		if (showKey)
			++messages
		if (showCapo)
			++messages
		if (messages > 0) {
			val remainingScreenSpace = nativeDeviceSettings.screenSize.height - twentyPercent * 2
			var spacePerMessageLine = floor((remainingScreenSpace / messages).toDouble()).toInt()
			spacePerMessageLine = min(spacePerMessageLine, tenPercent)
			var errorCounter = 0
			for (error in uniqueErrors) {
				startScreenStrings.add(
					ScreenString.create(
						error.toString(),
						paint,
						nativeDeviceSettings.screenSize.width,
						spacePerMessageLine,
						START_SCREEN_ERROR_COLOR,
						false
					)
				)
				++errorCounter
				--errorCount
				if (errorCounter == 5 && errorCount > 0) {
					startScreenStrings.add(
						ScreenString.create(
							String.format(
								BeatPrompter.appResources.getString(R.string.otherErrorCount),
								errorCount
							),
							paint,
							nativeDeviceSettings.screenSize.width,
							spacePerMessageLine,
							START_SCREEN_ERROR_COLOR,
							false
						)
					)
					break
				}
			}
			for (nonBlankComment in nonBlankCommentLines)
				startScreenStrings.add(
					ScreenString.create(
						nonBlankComment.text.trim(),
						paint,
						nativeDeviceSettings.screenSize.width,
						spacePerMessageLine,
						nonBlankComment.textColor,
						false
					)
				)
			if (showKey) {
				val keyString =
					BeatPrompter.appResources.getString(R.string.keyPrefix) + ": " + songLoadInfo.songInfo.keySignature
				startScreenStrings.add(
					ScreenString.create(
						keyString,
						paint,
						nativeDeviceSettings.screenSize.width,
						spacePerMessageLine,
						START_SCREEN_KEY_COLOR,
						false
					)
				)
			}
			if (showCapo) {
				val keyString =
					BeatPrompter.appResources.getString(R.string.capoPrefix) + " " + songLoadInfo.songInfo.capo
				startScreenStrings.add(
					ScreenString.create(
						keyString,
						paint,
						nativeDeviceSettings.screenSize.width,
						spacePerMessageLine,
						START_SCREEN_CAPO_COLOR,
						false
					)
				)
			}
			if (showBpm != ShowBPMContext.No) {
				val rounded =
					showBpm == ShowBPMContext.Rounded || songLoadInfo.songInfo.bpm == songLoadInfo.songInfo.bpm.toInt()
						.toDouble()
				var bpmString = BeatPrompter.appResources.getString(R.string.bpmPrefix) + ": "
				bpmString += if (rounded)
					songLoadInfo.songInfo.bpm.roundToInt()
				else
					songLoadInfo.songInfo.bpm
				startScreenStrings.add(
					ScreenString.create(
						bpmString,
						paint,
						nativeDeviceSettings.screenSize.width,
						spacePerMessageLine,
						START_SCREEN_BPM_COLOR,
						false
					)
				)
			}
		}
		if (songLoadInfo.songLoadMode !== ScrollingMode.Manual)
			startScreenStrings.add(
				ScreenString.create(
					BeatPrompter.appResources.getString(R.string.tapTwiceToStart),
					paint,
					nativeDeviceSettings.screenSize.width,
					tenPercent,
					START_SCREEN_TAP_TO_START_COLOR,
					true
				)
			)
		return startScreenStrings to nextSongString
	}

	private fun calculateStartAndStopScrollTimes(
		pauseTag: PauseTag?,
		lineStartTime: Long,
		lineDuration: Long,
		currentBeatEvents: EventBlock?,
		audioLatency: Int
	): Pair<Long, Long> {
		// Calculate when this line should start scrolling
		val startScrollTime =
			when (currentLineBeatInfo.scrollMode) {
				// Smooth mode? Start scrolling instantly.
				ScrollingMode.Smooth -> songTime
				else ->
					// Pause line? Start scrolling after 95% of the pause has elapsed.
					if (pauseTag != null)
						lineStartTime + (pauseTag.duration * 0.95).toLong()
					// Beat line? Start scrolling on the last beat.
					else
						currentBeatEvents!!.mEvents.lastOrNull()?.eventTime ?: songTime
				// (Manual mode ignores these scroll values)
			}
		// Calculate when the line should stop scrolling
		val stopScrollTime =
			when (currentLineBeatInfo.scrollMode) {
				// Smooth mode? It should stop scrolling once the allocated time has elapsed.
				ScrollingMode.Smooth -> songTime + lineDuration
				else ->
					// Pause line? It should stop scrolling when the pause has ran out
					if (pauseTag != null)
						lineStartTime + pauseTag.duration
					// Beat line? It should stop scrolling after the final beat
					else
						currentBeatEvents!!.mBlockEndTime
				// (Manual mode ignores these values)
			}

		// Events are going to be offset later to compensate for audio latency.
		// Lines, however, won't be. So we need to compensate NOW.
		val audioLatencyOffset = Utils.milliToNano(audioLatency)
		return Pair(
			startScrollTime + audioLatencyOffset,
			stopScrollTime + audioLatencyOffset
		)
	}

	private fun calculateLineDuration(
		pauseTag: PauseTag?,
		addToPause: Long,
		lineStartTime: Long,
		currentBeatEvents: EventBlock?
	): Long {
		// Calculate how long this line will last for.
		return when {
			// Pause line? Lasts as long as the pause!
			pauseTag != null -> pauseTag.duration + addToPause
			// Smooth line? We've counted the bars, so do the sums.
			currentLineBeatInfo.scrollMode == ScrollingMode.Smooth && timePerBar > 0 -> timePerBar * currentLineBeatInfo.bpl
			// Beat line? The result of generateBeatEvents will contain the time
			// that the beats end, so subtract the start time from that to get our duration.
			else -> currentBeatEvents!!.mBlockEndTime - lineStartTime
			// (Manual mode ignores these scroll values)
		}
	}

	/**
	 * An "event block" is simply a list of events, in chronological order, and a time that marks the point
	 * at which the block ends. Note that the end time is not necessarily the same as the time of the last
	 * event. For example, a block of five beat events (where each beat last n nanoseconds) will contain
	 * five events with the times of n*0, n*1, n*2, n*3, n*4, and the end time will be n*5, as a "beat event"
	 * actually covers the duration of the beat.
	 */
	private data class EventBlock(val mEvents: List<BaseEvent>, val mBlockEndTime: Long)

	private class LineList : ArrayList<Line>() {
		override fun add(element: Line): Boolean {
			val lastOrNull = lastOrNull()
			lastOrNull?.nextLine = element
			element.previousLine = lastOrNull
			return super.add(element)
		}
	}

	private class CircularGraphicsList : ArrayList<LineGraphic>() {
		override fun add(element: LineGraphic): Boolean {
			lastOrNull()?.nextGraphic = element
			val result = super.add(element)
			last().nextGraphic = first()
			return result
		}
	}

	private fun sortEvents(eventList: List<BaseEvent>): List<BaseEvent> {
		// Sort all events by time, and by type within that.
		return eventList.sortedWith { e1, e2 ->
			when {
				e1.eventTime > e2.eventTime -> 1
				e1.eventTime < e2.eventTime -> -1
				else -> {
					// StartEvents must appear before anything else, as their
					// function is a "starting point" for song processing to
					// continue from.
					if (e1 is StartEvent && e2 is StartEvent)
						0
					else if (e1 is StartEvent)
						-1
					else if (e2 is StartEvent)
						1
					// MIDI events are most important. We want to
					// these first at any given time for maximum MIDI
					// responsiveness
					else if (e1 is MidiEvent && e2 is MidiEvent)
						0
					else if (e1 is MidiEvent)
						-1
					else if (e2 is MidiEvent)
						1
					// AudioEvents are next-most important. We want to process
					// these first at any given time for maximum audio
					// responsiveness
					else if (e1 is AudioEvent && e2 is AudioEvent)
						0
					else if (e1 is AudioEvent)
						-1
					else if (e2 is AudioEvent)
						1
					// Now LineEvents for maximum visual responsiveness
					else if (e1 is LineEvent && e2 is LineEvent)
						0
					else if (e1 is LineEvent)
						-1
					else if (e2 is LineEvent)
						1
					// Remaining order doesn't really matter
					else
						0
				}
			}
		}
	}

	/**
	 * Each MIDIEvent might have an offset. Process that here.
	 */
	private fun offsetMIDIEvents(
		events: List<BaseEvent>
	): List<BaseEvent> {
		val beatEvents =
			events.asSequence().filterIsInstance<BeatEvent>().sortedBy { it.eventTime }.toList()
		return events.map {
			if (it is MidiEvent)
				offsetMIDIEvent(it, beatEvents)
			else
				it
		}
	}

	/**
	 * Each MIDIEvent might have an offset. Process that here.
	 */
	private fun offsetMIDIEvent(
		midiEvent: MidiEvent,
		beatEvents: List<BeatEvent>
	): MidiEvent =
		if (midiEvent.offset.amount != 0) {
			// OK, this event needs moved.
			var newTime: Long = -1
			if (midiEvent.offset.offsetType === EventOffsetType.Milliseconds) {
				val offset = Utils.milliToNano(midiEvent.offset.amount)
				newTime = midiEvent.eventTime + offset
			} else {
				// Offset by beat count.
				val beatCount = midiEvent.offset.amount
				val beatsBeforeOrAfterThisMIDIEvent = beatEvents.filter {
					if (beatCount >= 0)
						it.eventTime > midiEvent.eventTime
					else
						it.eventTime < midiEvent.eventTime
				}
				val beatsInOrder =
					if (beatCount < 0)
						beatsBeforeOrAfterThisMIDIEvent.reversed()
					else
						beatsBeforeOrAfterThisMIDIEvent
				val beatWeWant = beatsInOrder.asSequence().take(beatCount.absoluteValue).lastOrNull()
				if (beatWeWant != null)
					newTime = beatWeWant.eventTime
			}
			if (newTime < 0) {
				addError(
					ContentParsingError(
						midiEvent.offset.sourceFileLineNumber,
						R.string.midi_offset_is_before_start_of_song
					)
				)
				newTime = 0
			}
			MidiEvent(newTime, midiEvent.messages)
		} else
			midiEvent

	companion object {
		private val ONE_DECISECOND_IN_NANOSECONDS = Utils.milliToNano(100)

		private const val DEFAULT_START_SCREEN_COMMENT_COLOR = Color.WHITE
		private const val START_SCREEN_ERROR_COLOR = Color.RED
		private const val START_SCREEN_TITLE_COLOR = Color.YELLOW
		private const val START_SCREEN_ARTIST_COLOR = Color.YELLOW
		private const val START_SCREEN_BPM_COLOR = Color.CYAN
		private const val START_SCREEN_KEY_COLOR = Color.CYAN
		private const val START_SCREEN_CAPO_COLOR = Color.CYAN
		private const val START_SCREEN_TAP_TO_START_COLOR = Color.GREEN

		private fun BaseEvent.shouldCompensateForAudioLatency(lineEventFound: Boolean): Boolean =
			!(this is AudioEvent || this is StartEvent || this is ClickEvent || (this is LineEvent && !lineEventFound))

		private fun compensateForAudioLatency(
			events: List<BaseEvent>,
			nanoseconds: Long
		): List<BaseEvent> {
			// First line event should NOT be offset.
			var lineEventFound = false
			return events.map {
				(if (it.shouldCompensateForAudioLatency(lineEventFound))
					it.offset(nanoseconds)
				else
					it).also { event ->
					lineEventFound = lineEventFound || event is LineEvent
				}
			}
		}

		private fun generateClickEvents(
			events: List<BaseEvent>
		): List<BaseEvent> =
			events.flatMap {
				if (it is BeatEvent && it.click)
					listOf(it, ClickEvent(it.eventTime))
				else
					listOf(it)
			}
	}
}