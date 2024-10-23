package com.stevenfrew.beatprompter

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Paint
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cache.parse.SongInfoParser
import com.stevenfrew.beatprompter.cache.parse.SongParser
import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.graphics.Rect
import com.stevenfrew.beatprompter.midi.EventOffset
import com.stevenfrew.beatprompter.midi.EventOffsetType
import com.stevenfrew.beatprompter.mock.MockGlobalAppResources
import com.stevenfrew.beatprompter.mock.MockPreferences
import com.stevenfrew.beatprompter.mock.MockSupportFileResolver
import com.stevenfrew.beatprompter.mock.graphics.MockBitmapFactory
import com.stevenfrew.beatprompter.mock.graphics.MockFontManager
import com.stevenfrew.beatprompter.mock.graphics.MockLine
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
import com.stevenfrew.beatprompter.song.load.SongLoadInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.StringWriter
import java.nio.file.Paths
import java.util.Date
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.collections.forEach
import kotlin.io.path.pathString

object TestUtils {
	internal fun setMocks() {
		BeatPrompter.appResources = MockGlobalAppResources()
		BeatPrompter.preferences = MockPreferences()
		BeatPrompter.fontManager = MockFontManager()
		BeatPrompter.bitmapFactory = MockBitmapFactory()
	}

	internal fun getTestFile(subfolder: String, filename: String): File {
		var testFilePath = Paths.get(TEST_DATA_FOLDER_PATH.pathString, subfolder, filename)
		var testFile = File(testFilePath.pathString)
		if (testFile.exists() && testFile.isFile)
			return testFile
		throw UnsupportedOperationException("Requested test file ${testFilePath.pathString} could not found.")
	}

	internal fun parseSong(songFile: File): Pair<Song, List<FileParseError>> {
		val cachedFile =
			CachedFile(
				songFile,
				songFile.path,
				songFile.name,
				Date(songFile.lastModified()),
				listOf(songFile.parent ?: "")
			)
		val songFileInfoParser = SongInfoParser(cachedFile)
		val songFile = songFileInfoParser.parse()
		val songLoadInfo = SongLoadInfo(
			songFile, songFile.variations.first(), ScrollingMode.Beat, TestDisplaySettings,
			TestDisplaySettings
		)
		val songParser =
			SongParser(songLoadInfo, MockSupportFileResolver(TEST_DATA_FOLDER_PATH.pathString))
		val song = songParser.parse()
		return song to songParser.errors
	}

	internal fun testSongFileEvents(filename: String): Song {
		val songFile = getTestFile("songs", filename)
		val (song, errors) = parseSong(songFile)
		checkExpectedSongEvents(songFile, song, errors)
		return song
	}

	private fun checkExpectedSongEvents(songFile: File, song: Song, errors: List<FileParseError>) {
		val nameWithoutExtension = songFile.nameWithoutExtension
		val songEvents = getSongEvents(song)
		val expectedEventsPath =
			Paths.get(songFile.parent, "${nameWithoutExtension}.expectedEvents.xml").pathString
		val parsedEventsPath =
			Paths.get(songFile.parent, "${nameWithoutExtension}.parsedEvents.xml").pathString
		val expectedErrorsPath =
			Paths.get(songFile.parent, "${nameWithoutExtension}.expectedErrors.txt").pathString
		val expectedEventsXml = readEventListXml(expectedEventsPath)
		val expectedErrors = readErrorList(expectedErrorsPath)
		assertEquals(expectedErrors, errors)
		val eventsXml = getEventListAsXml(songEvents)
		File(parsedEventsPath).writeText(eventsXml)
		if (expectedEventsXml != null)
			assertEquals(
				expectedEventsXml,
				eventsXml,
				"$nameWithoutExtension did not parse correctly. Compare output XML files for details."
			)
		else
			File(expectedEventsPath).writeText(eventsXml)
	}

	private val TestScreenSize = Rect(0, 0, 2000, 1000)
	val TestDisplaySettings = DisplaySettings(ORIENTATION_LANDSCAPE, 8.0f, 80.0f, TestScreenSize)

	private val PROJECT_DIR_ABSOLUTE_PATH = Paths.get("").toAbsolutePath().toString()
	private val TEST_DATA_FOLDER_PATH = Paths.get(PROJECT_DIR_ABSOLUTE_PATH, "src/test/data")

	private fun getSongEvents(song: Song): List<BaseEvent> {
		val events = mutableListOf<BaseEvent>()
		var event: LinkedEvent? = song.currentEvent
		while (event != null) {
			events.add(event.event)
			event = event.nextEvent
		}
		return events
	}

	private fun readErrorList(filename: String): List<String> {
		val errorListFile = File(filename)
		if (errorListFile.exists() && errorListFile.isFile)
			return errorListFile.readLines()
		return listOf()
	}

	private fun readEventListXml(filename: String): String? {
		val eventListFile = File(filename)
		if (eventListFile.exists() && eventListFile.isFile)
			return eventListFile.readText()
		return null
	}

	private fun parseEventListXml(xmlString: String): List<BaseEvent> {
		val xml = DocumentBuilderFactory
			.newInstance()
			.newDocumentBuilder()
			.parse(xmlString)
		val childNodes = xml.documentElement.childNodes
		val childNodeCount = childNodes.length
		val childNodeList = (0..childNodeCount).map {
			childNodes.item(it) as? Element
		}.filterIsInstance<Element>()
		return childNodeList.map { parseXmlElement(it) }
	}

	private fun getEventListAsXml(events: List<BaseEvent>): String {
		val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
		val document = docBuilder.newDocument()
		val root = document.createElement("events")
		document.appendChild(root)
		events.forEach {
			val eventElement = writeEventToXml(it, document)
			eventElement.setAttribute(TIME_ATTRIBUTE, it.eventTime.toString())
			root.appendChild(eventElement)
		}
		val transformer = TransformerFactory.newInstance().newTransformer()
		transformer.setOutputProperty(OutputKeys.INDENT, "yes")
		val stringWriter = StringWriter()
		val output = StreamResult(stringWriter)
		val input = DOMSource(document)
		transformer.transform(input, output)
		return stringWriter.toString()
	}

	private fun parseXmlElement(element: Element): BaseEvent =
		when (element.tagName) {
			AUDIO_EVENT_TAG_NAME -> parseAudioEventXmlElement(element)
			BEAT_EVENT_TAG_NAME -> parseBeatEventXmlElement(element)
			CLICK_EVENT_TAG_NAME -> parseClickEventXmlElement(element)
			COMMENT_EVENT_TAG_NAME -> parseCommentEventXmlElement(element)
			END_EVENT_TAG_NAME -> parseEndEventXmlElement(element)
			LINE_EVENT_TAG_NAME -> parseLineEventXmlElement(element)
			MIDI_EVENT_TAG_NAME -> parseMidiEventXmlElement(element)
			PAUSE_EVENT_TAG_NAME -> parsePauseEventXmlElement(element)
			START_EVENT_TAG_NAME -> parseStartEventXmlElement(element)
			else -> throw UnsupportedOperationException("Events file contained an unknown event element '${element.tagName}'")
		}

	private fun writeEventToXml(event: BaseEvent, document: Document): Element =
		when (event) {
			is AudioEvent -> writeAudioEventToXml(event, document)
			is BeatEvent -> writeBeatEventToXml(event, document)
			is ClickEvent -> writeClickEventToXml(document)
			is CommentEvent -> writeCommentEventToXml(event, document)
			is EndEvent -> writeEndEventToXml(document)
			is LineEvent -> writeLineEventToXml(event, document)
			is MidiEvent -> writeMidiEventToXml(event, document)
			is PauseEvent -> writePauseEventToXml(event, document)
			is StartEvent -> writeStartEventToXml(document)
			else -> throw UnsupportedOperationException("Don't know how to handle this event type.")
		}

	private const val AUDIO_EVENT_TAG_NAME = "audio"
	private const val BEAT_EVENT_TAG_NAME = "beat"
	private const val CLICK_EVENT_TAG_NAME = "click"
	private const val COMMENT_EVENT_TAG_NAME = "comment"
	private const val END_EVENT_TAG_NAME = "end"
	private const val LINE_EVENT_TAG_NAME = "line"
	private const val MIDI_EVENT_TAG_NAME = "midi"
	private const val PAUSE_EVENT_TAG_NAME = "pause"
	private const val START_EVENT_TAG_NAME = "start"

	private const val TIME_ATTRIBUTE = "time"
	private const val VOLUME_ATTRIBUTE = "volume"
	private const val DURATION_ATTRIBUTE = "duration"
	private const val IS_BACKING_TRACK_ATTRIBUTE = "isBackingTrack"
	private const val BPM_ATTRIBUTE = "bpm"
	private const val BPB_ATTRIBUTE = "bpb"
	private const val BEAT_ATTRIBUTE = "beat"
	private const val CLICK_ATTRIBUTE = "click"
	private const val WILL_SCROLL_ON_BEAT_ATTRIBUTE = "willScrollOnBeat"
	private const val TEXT_ATTRIBUTE = "text"
	private const val AUDIENCE_ATTRIBUTE = "audience"
	private const val MESSAGES_ATTRIBUTE = "messages"
	private const val OFFSET_AMOUNT_ATTRIBUTE = "offsetAmount"
	private const val OFFSET_TYPE_ATTRIBUTE = "offsetType"
	private const val LINE_NUMBER_ATTRIBUTE = "lineNumber"
	private const val BEATS_ATTRIBUTE = "beats"
	private const val PATH_ATTRIBUTE = "path"
	private const val FILENAME_ATTRIBUTE = "filename"
	private const val ID_ATTRIBUTE = "id"
	private const val IS_IN_CHORUS_SECTION_ATTRIBUTE = "isInChorusSection"
	private const val SCROLLING_MODE_ATTRIBUTE = "scrollingMode"
	private const val SONG_PIXEL_POSITION_ATTRIBUTE = "songPixelPosition"
	private const val Y_START_SCROLL_TIME_ATTRIBUTE = "yStartScrollTime"
	private const val Y_STOP_SCROLL_TIME_ATTRIBUTE = "yStopScrollTime"
	private const val LINES_ATTRIBUTE = "lines"
	private const val WIDTH_ATTRIBUTE = "width"
	private const val HEIGHT_ATTRIBUTE = "height"
	private const val JUMP_SCROLL_INTERVALS_ATTRIBUTE = "jumpScrollIntervals"
	private const val PIXELS_TO_TIMES_ATTRIBUTE = "pixelsToTimes"
	private const val GRAPHIC_HEIGHTS_ATTRIBUTE = "graphicHeights"

	private fun parseAudioEventXmlElement(element: Element): AudioEvent {
		val time = element.getAttribute(TIME_ATTRIBUTE).toLong()
		val volume = element.getAttribute(VOLUME_ATTRIBUTE).toInt()
		val duration = element.getAttribute(DURATION_ATTRIBUTE).toLong()
		val isBackingTrack = element.getAttribute(IS_BACKING_TRACK_ATTRIBUTE).toBoolean()
		val path = element.getAttribute(PATH_ATTRIBUTE)
		val filename = element.getAttribute(FILENAME_ATTRIBUTE)
		val id = element.getAttribute(ID_ATTRIBUTE)
		val cachedFile = CachedFile(File(path), id, filename, Date(), listOf())
		val audioFile = AudioFile(cachedFile, duration)
		return AudioEvent(time, audioFile, volume, isBackingTrack)
	}

	private fun writeAudioEventToXml(event: AudioEvent, document: Document): Element {
		val element = document.createElement(AUDIO_EVENT_TAG_NAME)
		element.setAttribute(VOLUME_ATTRIBUTE, event.volume.toString())
		element.setAttribute(DURATION_ATTRIBUTE, event.audioFile.duration.toString())
		element.setAttribute(IS_BACKING_TRACK_ATTRIBUTE, event.isBackingTrack.toString())
		element.setAttribute(PATH_ATTRIBUTE, event.audioFile.file.path)
		element.setAttribute(FILENAME_ATTRIBUTE, event.audioFile.file.name)
		element.setAttribute(ID_ATTRIBUTE, event.audioFile.id)
		return element
	}

	private fun parseBeatEventXmlElement(element: Element): BeatEvent {
		val time = element.getAttribute(TIME_ATTRIBUTE).toLong()
		val bpm = element.getAttribute(BPM_ATTRIBUTE).toDouble()
		val bpb = element.getAttribute(BPB_ATTRIBUTE).toInt()
		val beat = element.getAttribute(BEAT_ATTRIBUTE).toInt()
		val click = element.getAttribute(CLICK_ATTRIBUTE).toBoolean()
		val willScrollOnBeat = element.getAttribute(WILL_SCROLL_ON_BEAT_ATTRIBUTE).toInt()
		return BeatEvent(time, bpm, bpb, beat, click, willScrollOnBeat)
	}

	private fun writeBeatEventToXml(event: BeatEvent, document: Document): Element {
		val element = document.createElement(BEAT_EVENT_TAG_NAME)
		element.setAttribute(BPM_ATTRIBUTE, event.bpm.toString())
		element.setAttribute(BPB_ATTRIBUTE, event.bpb.toString())
		element.setAttribute(BEAT_ATTRIBUTE, event.beat.toString())
		element.setAttribute(CLICK_ATTRIBUTE, event.click.toString())
		element.setAttribute(WILL_SCROLL_ON_BEAT_ATTRIBUTE, event.willScrollOnBeat.toString())
		return element
	}

	private fun parseClickEventXmlElement(element: Element): ClickEvent {
		val time = element.getAttribute(TIME_ATTRIBUTE).toLong()
		return ClickEvent(time)
	}

	private fun writeClickEventToXml(document: Document): Element {
		val element = document.createElement(CLICK_EVENT_TAG_NAME)
		return element
	}

	private fun parseCommentEventXmlElement(element: Element): CommentEvent {
		val time = element.getAttribute(TIME_ATTRIBUTE).toLong()
		val text = element.getAttribute(TEXT_ATTRIBUTE)
		val audience = element.getAttribute(AUDIENCE_ATTRIBUTE)
		return CommentEvent(time, Song.Comment(text, audience.split("***"), TestScreenSize, Paint()))
	}

	private fun writeCommentEventToXml(event: CommentEvent, document: Document): Element {
		val element = document.createElement(COMMENT_EVENT_TAG_NAME)
		element.setAttribute(TEXT_ATTRIBUTE, event.comment.text)
		element.setAttribute(AUDIENCE_ATTRIBUTE, "")
		return element
	}

	private fun parseEndEventXmlElement(element: Element): EndEvent {
		val time = element.getAttribute(TIME_ATTRIBUTE).toLong()
		return EndEvent(time)
	}

	private fun writeEndEventToXml(document: Document): Element {
		val element = document.createElement(END_EVENT_TAG_NAME)
		return element
	}

	private fun parseLineEventXmlElement(element: Element): LineEvent {
		val time = element.getAttribute(TIME_ATTRIBUTE).toLong()
		val duration = element.getAttribute(TIME_ATTRIBUTE).toLong()
		val isInChorusSection = element.getAttribute(TIME_ATTRIBUTE).toBoolean()
		val scrollingMode = ScrollingMode.valueOf(element.getAttribute(TIME_ATTRIBUTE))
		val songPixelPosition = element.getAttribute(SONG_PIXEL_POSITION_ATTRIBUTE).toInt()
		val yStartScrollTime = element.getAttribute(Y_START_SCROLL_TIME_ATTRIBUTE).toLong()
		val yStopScrollTime = element.getAttribute(Y_STOP_SCROLL_TIME_ATTRIBUTE).toLong()

		return LineEvent(
			time,
			MockLine(
				time,
				duration,
				scrollingMode,
				songPixelPosition,
				isInChorusSection,
				yStartScrollTime,
				yStopScrollTime,
				TestDisplaySettings
			)
		)
	}

	private fun writeLineEventToXml(event: LineEvent, document: Document): Element {
		val element = document.createElement(LINE_EVENT_TAG_NAME)
		element.setAttribute(DURATION_ATTRIBUTE, event.line.lineDuration.toString())
		element.setAttribute(IS_IN_CHORUS_SECTION_ATTRIBUTE, event.line.isInChorusSection.toString())
		element.setAttribute(SCROLLING_MODE_ATTRIBUTE, event.line.scrollMode.toString())
		element.setAttribute(SONG_PIXEL_POSITION_ATTRIBUTE, event.line.songPixelPosition.toString())
		element.setAttribute(Y_START_SCROLL_TIME_ATTRIBUTE, event.line.yStartScrollTime.toString())
		element.setAttribute(Y_STOP_SCROLL_TIME_ATTRIBUTE, event.line.yStopScrollTime.toString())
		element.setAttribute(LINES_ATTRIBUTE, event.line.measurements.lines.toString())
		element.setAttribute(WIDTH_ATTRIBUTE, event.line.measurements.lineWidth.toString())
		element.setAttribute(HEIGHT_ATTRIBUTE, event.line.measurements.lineHeight.toString())
		element.setAttribute(
			JUMP_SCROLL_INTERVALS_ATTRIBUTE,
			event.line.measurements.jumpScrollIntervals.joinToString(",")
		)
		element.setAttribute(
			PIXELS_TO_TIMES_ATTRIBUTE,
			event.line.measurements.pixelsToTimes.joinToString(",")
		)
		element.setAttribute(
			GRAPHIC_HEIGHTS_ATTRIBUTE,
			event.line.measurements.graphicHeights.joinToString(",")
		)
		return element
	}

	private fun parseMidiEventXmlElement(element: Element): MidiEvent {
		val time = element.getAttribute(TIME_ATTRIBUTE).toLong()
		val messages = element.getAttribute(MESSAGES_ATTRIBUTE)
		val offsetAmount = element.getAttribute(OFFSET_AMOUNT_ATTRIBUTE).toInt()
		val offsetType = EventOffsetType.valueOf(element.getAttribute(OFFSET_TYPE_ATTRIBUTE))
		val offsetLineNumber = element.getAttribute(LINE_NUMBER_ATTRIBUTE).toInt()
		return MidiEvent(
			time,
			messages.split(',').map { messageBytes ->
				MidiMessage(
					messageBytes.split(' ').map {
						it.toByte(16)
					}.toByteArray()
				)
			},
			EventOffset(offsetAmount, offsetType, offsetLineNumber)
		)
	}

	private fun writeMidiEventToXml(event: MidiEvent, document: Document): Element {
		val element = document.createElement(LINE_EVENT_TAG_NAME)
		element.setAttribute(OFFSET_AMOUNT_ATTRIBUTE, event.offset.amount.toString())
		element.setAttribute(OFFSET_TYPE_ATTRIBUTE, event.offset.offsetType.toString())
		element.setAttribute(LINE_NUMBER_ATTRIBUTE, event.offset.sourceFileLineNumber.toString())
		val messagesString = event.messages.map { message ->
			message.bytes.map {
				it.toString(16)
			}.joinToString(" ")
		}.joinToString(",")
		element.setAttribute(MESSAGES_ATTRIBUTE, messagesString)
		return element
	}

	private fun parsePauseEventXmlElement(element: Element): PauseEvent {
		val time = element.getAttribute(TIME_ATTRIBUTE).toLong()
		val beats = element.getAttribute(BEATS_ATTRIBUTE).toInt()
		val beat = element.getAttribute(BEAT_ATTRIBUTE).toInt()
		return PauseEvent(time, beats, beat)
	}

	private fun writePauseEventToXml(event: PauseEvent, document: Document): Element {
		val element = document.createElement(PAUSE_EVENT_TAG_NAME)
		element.setAttribute(BEATS_ATTRIBUTE, event.beats.toString())
		element.setAttribute(BEAT_ATTRIBUTE, event.beat.toString())
		return element
	}

	private fun parseStartEventXmlElement(element: Element): StartEvent {
		val time = element.getAttribute(TIME_ATTRIBUTE).toLong()
		return StartEvent(time)
	}

	private fun writeStartEventToXml(document: Document): Element {
		val element = document.createElement(START_EVENT_TAG_NAME)
		return element
	}
}
