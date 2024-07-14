package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.midi.TriggerType
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.util.normalize
import org.w3c.dom.Document
import org.w3c.dom.Element

@CacheXmlTag("song")
/**
 * A song file in the cache.
 */
class SongFile(
	cachedFile: CachedFile,
	val mLines: Int,
	val mBars: Int,
	val mTitle: String,
	val mArtist: String,
	val mKey: String,
	val mBPM: Double,
	val mDuration: Long,
	val mMixedMode: Boolean,
	val mTotalPauses: Long,
	val mAudioFiles: Map<String, List<String>>,
	val mImageFiles: List<String>,
	val mTags: Set<String>,
	val mProgramChangeTrigger: SongTrigger,
	val mSongSelectTrigger: SongTrigger,
	val mFilterOnly: Boolean,
	val mRating: Int,
	val mVariations: List<String>,
	errors: List<FileParseError>
) : CachedTextFile(cachedFile, errors) {
	val mNormalizedArtist = mArtist.normalize()
	val mNormalizedTitle = mTitle.normalize()
	val mSortableArtist = sortableString(mArtist)
	val mSortableTitle = sortableString(mTitle)
	val isSmoothScrollable
		get() = mDuration > 0
	val isBeatScrollable
		get() = mBPM > 0.0
	val bestScrollingMode
		get() = when {
			isBeatScrollable -> ScrollingMode.Beat
			isSmoothScrollable -> ScrollingMode.Smooth
			else -> ScrollingMode.Manual
		}

	fun matchesTrigger(trigger: SongTrigger): Boolean {
		return mSongSelectTrigger == trigger || mProgramChangeTrigger == trigger
	}

	override fun writeToXML(doc: Document, element: Element) {
		super.writeToXML(doc, element)
		element.setAttribute(TITLE_ATTRIBUTE, mTitle)
		element.setAttribute(ARTIST_ATTRIBUTE, mArtist)
		element.setAttribute(LINES_ATTRIBUTE, "$mLines")
		element.setAttribute(BARS_ATTRIBUTE, "$mBars")
		element.setAttribute(KEY_ATTRIBUTE, mKey)
		element.setAttribute(BPM_ATTRIBUTE, "$mBPM")
		element.setAttribute(DURATION_ATTRIBUTE, "$mDuration")
		element.setAttribute(MIXED_MODE_ATTRIBUTE, "$mMixedMode")
		element.setAttribute(TOTAL_PAUSES_ATTRIBUTE, "$mTotalPauses")
		element.setAttribute(FILTER_ONLY_ATTRIBUTE, "$mFilterOnly")
		element.setAttribute(RATING_ATTRIBUTE, "$mRating")

		writeStringsToElement(doc, element, TAG_TAG, mTags)
		writeStringsToElement(doc, element, IMAGE_FILE_TAG, mImageFiles)
		writeStringsToElement(doc, element, VARIATION_TAG, mVariations)

		writeAudioFilesToElement(doc, element, mAudioFiles)
		writeSongTriggerToElement(doc, element, SONG_SELECT_TRIGGER_TAG, mSongSelectTrigger)
		writeSongTriggerToElement(doc, element, PROGRAM_CHANGE_TRIGGER_TAG, mProgramChangeTrigger)
	}

	companion object {
		private var thePrefix = "${BeatPrompter.getResourceString(R.string.lowerCaseThe)} "

		private const val TITLE_ATTRIBUTE = "title"
		private const val ARTIST_ATTRIBUTE = "artist"
		private const val LINES_ATTRIBUTE = "lines"
		private const val BARS_ATTRIBUTE = "bars"
		private const val KEY_ATTRIBUTE = "key"
		private const val BPM_ATTRIBUTE = "bpm"
		private const val DURATION_ATTRIBUTE = "duration"
		private const val MIXED_MODE_ATTRIBUTE = "mixedMode"
		private const val TOTAL_PAUSES_ATTRIBUTE = "totalPauses"
		private const val FILTER_ONLY_ATTRIBUTE = "filterOnly"
		private const val RATING_ATTRIBUTE = "rating"

		private const val IMAGE_FILE_TAG = "imageFiles"
		private const val TAG_TAG = "tags"
		private const val VARIATION_TAG = "variations"

		private const val AUDIO_FILE_TAG = "audioFiles"
		private const val VARIATION_ATTRIBUTE = "variation"
		private const val AUDIO_FILES_FOR_VARIATION_TAG = "audioFilesForVariation"

		private const val PROGRAM_CHANGE_TRIGGER_TAG = "programChangeTrigger"
		private const val SONG_SELECT_TRIGGER_TAG = "songSelectTrigger"

		fun readSongInfoFromAttributes(element: Element?, cachedFile: CachedFile): SongFile? {
			if (element?.hasAttribute(TITLE_ATTRIBUTE) == true && element.hasAttribute(ARTIST_ATTRIBUTE)) {
				val title = element.getAttribute(TITLE_ATTRIBUTE)
				val artist = element.getAttribute(ARTIST_ATTRIBUTE)
				val key = element.getAttribute(KEY_ATTRIBUTE)
				val linesString = element.getAttribute(LINES_ATTRIBUTE)
				val barsString = element.getAttribute(BARS_ATTRIBUTE)
				val bpmString = element.getAttribute(BPM_ATTRIBUTE)
				val durationString = element.getAttribute(DURATION_ATTRIBUTE)
				val mixedModeString = element.getAttribute(MIXED_MODE_ATTRIBUTE)
				val totalPausesString = element.getAttribute(TOTAL_PAUSES_ATTRIBUTE)
				val filterOnlyString = element.getAttribute(FILTER_ONLY_ATTRIBUTE)
				val ratingString = element.getAttribute(RATING_ATTRIBUTE)
				try {
					val lines = linesString.toInt()
					val bars = barsString.toInt()
					val bpm = bpmString.toDouble()
					val duration = durationString.toLong()
					val mixedMode = mixedModeString.toBoolean()
					val totalPauses = totalPausesString.toLong()
					val filterOnly = filterOnlyString.toBoolean()
					val rating = ratingString.toInt()

					val tags = getStringsFromElement(element, TAG_TAG).toSet()
					val imageFiles = getStringsFromElement(element, IMAGE_FILE_TAG)
					val variations = getStringsFromElement(element, VARIATION_TAG)

					val audioFiles = getAudioFilesFromElement(element)

					val programChangeTrigger = getSongTriggerFromElement(
						element,
						PROGRAM_CHANGE_TRIGGER_TAG,
						TriggerType.ProgramChange
					)
					val songSelectTrigger =
						getSongTriggerFromElement(element, SONG_SELECT_TRIGGER_TAG, TriggerType.SongSelect)

					return SongFile(
						cachedFile,
						lines,
						bars,
						title,
						artist,
						key,
						bpm,
						duration,
						mixedMode,
						totalPauses,
						audioFiles,
						imageFiles,
						tags,
						programChangeTrigger ?: SongTrigger.DEAD_TRIGGER,
						songSelectTrigger ?: SongTrigger.DEAD_TRIGGER,
						filterOnly,
						rating,
						variations,
						listOf()
					)
				} catch (numberFormatException: NumberFormatException) {
					// Attribute is garbage, we'll need to actually examine the file.
				}
			}
			return null
		}

		private fun getAudioFilesFromElement(element: Element): Map<String, List<String>> {
			val tagElements = element.getElementsByTagName(AUDIO_FILES_FOR_VARIATION_TAG)
			val map = mutableMapOf<String, List<String>>()
			var counter = 0
			while (counter < tagElements.length) {
				val tagElement = tagElements.item(counter) as Element
				val variationName = tagElement.getAttribute(VARIATION_ATTRIBUTE)
				val audioFiles = getStringsFromElement(tagElement, AUDIO_FILE_TAG)
				map[variationName] = audioFiles
				counter += 1
			}
			return map
		}

		private fun getSongTriggerFromElement(
			element: Element,
			tag: String,
			type: TriggerType
		): SongTrigger? {
			val tagElements = element.getElementsByTagName(tag)
			if (tagElements.length > 0) {
				val tagElement = tagElements.item(0) as Element
				return SongTrigger.readFromXml(tagElement, type)
			}
			return null
		}

		private fun getStringsFromElement(element: Element, tag: String): List<String> {
			val tagElements = element.getElementsByTagName(tag)
			var counter = 0
			val result = mutableListOf<String>()
			while (counter < tagElements.length) {
				val tagElement = tagElements.item(counter)
				result.add(tagElement.textContent)
				counter += 1
			}
			return result
		}

		private fun writeStringsToElement(
			doc: Document,
			element: Element,
			tag: String,
			values: Iterable<String>
		) {
			values.forEach {
				val newElement = doc.createElement(tag)
				newElement.textContent = it
				element.appendChild(newElement)
			}
		}

		private fun writeAudioFilesToElement(
			doc: Document,
			element: Element,
			audioFiles: Map<String, List<String>>
		) {
			audioFiles.forEach {
				val newElement = doc.createElement(AUDIO_FILES_FOR_VARIATION_TAG)
				newElement.setAttribute(VARIATION_TAG, it.key)
				writeStringsToElement(doc, element, AUDIO_FILE_TAG, it.value)
				element.appendChild(newElement)
			}
		}

		private fun writeSongTriggerToElement(
			doc: Document,
			element: Element,
			tag: String,
			songTrigger: SongTrigger
		) {
			val newElement = doc.createElement(tag)
			songTrigger.writeToXML(newElement)
			element.appendChild(newElement)
		}

		fun sortableString(inStr: String?): String {
			return inStr?.lowercase()?.removePrefix(thePrefix) ?: ""
		}
	}
}