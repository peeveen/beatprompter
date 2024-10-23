package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.midi.TriggerType
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.chord.KeySignatureDefinition
import com.stevenfrew.beatprompter.util.normalize
import org.w3c.dom.Document
import org.w3c.dom.Element

@CacheXmlTag("song")
/**
 * A song file in the cache.
 */
class SongFile(
	cachedFile: CachedFile,
	val lines: Int,
	val bars: Int,
	val title: String,
	val artist: String,
	val key: String?,
	val bpm: Double,
	val duration: Long,
	val mixedModeVariations: List<String>,
	val totalPauseDuration: Long,
	val audioFiles: Map<String, List<String>>,
	val imageFiles: List<String>,
	val tags: Set<String>,
	val programChangeTrigger: SongTrigger,
	val songSelectTrigger: SongTrigger,
	val isFilterOnly: Boolean,
	val rating: Int,
	val variations: List<String>,
	val chords: List<String>,
	val firstChord: String?,
	errors: List<FileParseError>
) : CachedTextFile(cachedFile, errors) {
	val normalizedArtist = artist.normalize()
	val normalizedTitle = title.normalize()
	val sortableArtist = sortableString(artist)
	val sortableTitle = sortableString(title)
	val isSmoothScrollable
		get() = duration > 0
	val isBeatScrollable
		get() = bpm > 0.0
	val bestScrollingMode
		get() = when {
			isBeatScrollable -> ScrollingMode.Beat
			isSmoothScrollable -> ScrollingMode.Smooth
			else -> ScrollingMode.Manual
		}

	fun matchesTrigger(trigger: SongTrigger): Boolean =
		songSelectTrigger == trigger || programChangeTrigger == trigger

	val defaultVariation: String
		get() =
			BeatPrompter.preferences.preferredVariation.let {
				if (variations.contains(it)) it else variations.firstOrNull() ?: ""
			}

	override fun writeToXML(doc: Document, element: Element) {
		super.writeToXML(doc, element)
		element.setAttribute(TITLE_ATTRIBUTE, title)
		element.setAttribute(ARTIST_ATTRIBUTE, artist)
		element.setAttribute(LINES_ATTRIBUTE, "$lines")
		element.setAttribute(BARS_ATTRIBUTE, "$bars")
		key?.also { element.setAttribute(KEY_ATTRIBUTE, key) }
		element.setAttribute(BPM_ATTRIBUTE, "$bpm")
		element.setAttribute(DURATION_ATTRIBUTE, "$duration")
		element.setAttribute(TOTAL_PAUSES_ATTRIBUTE, "$totalPauseDuration")
		element.setAttribute(FILTER_ONLY_ATTRIBUTE, "$isFilterOnly")
		element.setAttribute(RATING_ATTRIBUTE, "$rating")
		firstChord?.also { element.setAttribute(FIRST_CHORD_ATTRIBUTE, firstChord) }

		writeStringsToElement(doc, element, TAGS_TAG, tags)
		writeStringsToElement(doc, element, MIXED_MODE_VARIATIONS_TAG, mixedModeVariations)
		writeStringsToElement(doc, element, IMAGE_FILES_TAG, imageFiles)
		writeStringsToElement(doc, element, VARIATIONS_TAG, variations)
		writeStringsToElement(doc, element, CHORDS_TAG, chords.toSet())

		writeAudioFilesToElement(doc, element, audioFiles)
		if (!songSelectTrigger.isDeadTrigger)
			writeSongTriggerToElement(doc, element, SONG_SELECT_TRIGGER_TAG, songSelectTrigger)
		if (!programChangeTrigger.isDeadTrigger)
			writeSongTriggerToElement(doc, element, PROGRAM_CHANGE_TRIGGER_TAG, programChangeTrigger)
	}

	val keySignature: String?
		get() = KeySignatureDefinition.getKeySignature(key, firstChord)
			?.getDisplayString(BeatPrompter.preferences.displayUnicodeAccidentals)

	companion object {
		private var thePrefix = "${BeatPrompter.appResources.getString(R.string.lowerCaseThe)} "

		private const val TITLE_ATTRIBUTE = "title"
		private const val ARTIST_ATTRIBUTE = "artist"
		private const val LINES_ATTRIBUTE = "lines"
		private const val BARS_ATTRIBUTE = "bars"
		private const val KEY_ATTRIBUTE = "key"
		private const val BPM_ATTRIBUTE = "bpm"
		private const val DURATION_ATTRIBUTE = "duration"
		private const val TOTAL_PAUSES_ATTRIBUTE = "totalPauses"
		private const val FILTER_ONLY_ATTRIBUTE = "filterOnly"
		private const val RATING_ATTRIBUTE = "rating"
		private const val FIRST_CHORD_ATTRIBUTE = "firstChord"

		private const val IMAGE_FILES_TAG = "imageFiles"
		private const val CHORDS_TAG = "chords"
		private const val TAGS_TAG = "tags"
		private const val MIXED_MODE_VARIATIONS_TAG = "mixedModeVariations"
		private const val VARIATIONS_TAG = "variations"

		private const val AUDIO_FILE_TAG = "audioFiles"
		private const val VARIATION_ATTRIBUTE = "variation"
		private const val AUDIO_FILES_FOR_VARIATION_TAG = "audioFilesForVariation"

		private const val PROGRAM_CHANGE_TRIGGER_TAG = "programChangeTrigger"
		private const val SONG_SELECT_TRIGGER_TAG = "songSelectTrigger"

		fun readSongInfoFromAttributes(element: Element?, cachedFile: CachedFile): SongFile? =
			if (element?.hasAttribute(TITLE_ATTRIBUTE) == true && element.hasAttribute(ARTIST_ATTRIBUTE)) {
				val title = element.getAttribute(TITLE_ATTRIBUTE)
				val artist = element.getAttribute(ARTIST_ATTRIBUTE)
				var key: String? = element.getAttribute(KEY_ATTRIBUTE)
				if (key.isNullOrBlank())
					key = null
				val linesString = element.getAttribute(LINES_ATTRIBUTE)
				val barsString = element.getAttribute(BARS_ATTRIBUTE)
				val bpmString = element.getAttribute(BPM_ATTRIBUTE)
				val durationString = element.getAttribute(DURATION_ATTRIBUTE)
				val totalPausesString = element.getAttribute(TOTAL_PAUSES_ATTRIBUTE)
				val filterOnlyString = element.getAttribute(FILTER_ONLY_ATTRIBUTE)
				val ratingString = element.getAttribute(RATING_ATTRIBUTE)
				var firstChord: String? = element.getAttribute(FIRST_CHORD_ATTRIBUTE)
				if (firstChord.isNullOrBlank())
					firstChord = null
				try {
					val lines = linesString.toInt()
					val bars = barsString.toInt()
					val bpm = bpmString.toDouble()
					val duration = durationString.toLong()
					val totalPauses = totalPausesString.toLong()
					val filterOnly = filterOnlyString.toBoolean()
					val rating = ratingString.toInt()

					val tags = getStringsFromElement(element, TAGS_TAG).toSet()
					val imageFiles = getStringsFromElement(element, IMAGE_FILES_TAG)
					val variations = getStringsFromElement(element, VARIATIONS_TAG)
					val chords = getStringsFromElement(element, CHORDS_TAG)
					val mixedModeVariations = getStringsFromElement(element, MIXED_MODE_VARIATIONS_TAG)

					val audioFiles = getAudioFilesFromElement(element)

					val programChangeTrigger = getSongTriggerFromElement(
						element,
						PROGRAM_CHANGE_TRIGGER_TAG,
						TriggerType.ProgramChange
					) ?: SongTrigger.DEAD_TRIGGER
					val songSelectTrigger =
						getSongTriggerFromElement(element, SONG_SELECT_TRIGGER_TAG, TriggerType.SongSelect)
							?: SongTrigger.DEAD_TRIGGER

					SongFile(
						cachedFile,
						lines,
						bars,
						title,
						artist,
						key,
						bpm,
						duration,
						mixedModeVariations,
						totalPauses,
						audioFiles,
						imageFiles,
						tags,
						programChangeTrigger,
						songSelectTrigger,
						filterOnly,
						rating,
						variations,
						chords,
						firstChord,
						listOf()
					)
				} catch (_: NumberFormatException) {
					// Attribute is garbage, we'll need to actually examine the file.
					null
				}
			} else null

		private fun getAudioFilesFromElement(element: Element): Map<String, List<String>> =
			mutableMapOf<String, List<String>>().apply {
				element.getElementsByTagName(AUDIO_FILES_FOR_VARIATION_TAG).also {
					repeat(it.length) { index ->
						val tagElement = it.item(index) as Element
						val variationName = tagElement.getAttribute(VARIATION_ATTRIBUTE)
						val audioFiles = getStringsFromElement(tagElement, AUDIO_FILE_TAG)
						this[variationName] = audioFiles
					}
				}
			}

		private fun getSongTriggerFromElement(
			element: Element,
			tag: String,
			type: TriggerType
		): SongTrigger? =
			element.getElementsByTagName(tag).takeIf { it.length > 0 }?.let {
				val tagElement = it.item(0) as Element
				return SongTrigger.readFromXml(tagElement, type)
			}

		private fun getStringsFromElement(element: Element, tag: String): List<String> =
			mutableListOf<String>().apply {
				element.getElementsByTagName(tag).run {
					repeat(length) {
						val tagElement = item(it)
						add(tagElement.textContent)
					}
				}
			}

		private fun writeStringsToElement(
			doc: Document,
			element: Element,
			tag: String,
			values: Iterable<String>
		) = values.forEach {
			doc.createElement(tag).apply {
				textContent = it
				element.appendChild(this)
			}
		}

		private fun writeAudioFilesToElement(
			doc: Document,
			element: Element,
			audioFiles: Map<String, List<String>>
		) = audioFiles.forEach {
			doc.createElement(AUDIO_FILES_FOR_VARIATION_TAG).apply {
				setAttribute(VARIATIONS_TAG, it.key)
				writeStringsToElement(doc, this, AUDIO_FILE_TAG, it.value)
				element.appendChild(this)
			}
		}

		private fun writeSongTriggerToElement(
			doc: Document,
			element: Element,
			tag: String,
			songTrigger: SongTrigger
		) = doc.createElement(tag).run {
			songTrigger.writeToXML(this)
			element.appendChild(this)
		}

		fun sortableString(inStr: String?): String = inStr?.lowercase()?.removePrefix(thePrefix) ?: ""
	}
}