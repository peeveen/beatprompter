package com.stevenfrew.beatprompter.cache

import org.w3c.dom.Document
import org.w3c.dom.Element

@CacheXmlTag("audiofile")
/**
 * An audio file from our cache of files.
 */
class AudioFile internal constructor(
	cachedFile: CachedFile,
	val duration: Long
) : CachedFile(cachedFile) {
	override fun writeToXML(doc: Document, element: Element) {
		super.writeToXML(doc, element)
		element.setAttribute(AUDIO_FILE_LENGTH_ATTRIBUTE, "$duration")
	}

	companion object {
		private const val AUDIO_FILE_LENGTH_ATTRIBUTE = "audioLengthMilliseconds"

		fun readAudioFileLengthFromAttribute(element: Element?): Long? {
			if (element?.hasAttribute(AUDIO_FILE_LENGTH_ATTRIBUTE) == true) {
				val lengthString = element.getAttribute(AUDIO_FILE_LENGTH_ATTRIBUTE)
				try {
					return lengthString.toLong()
				} catch (numberFormatException: NumberFormatException) {
					// Attribute is garbage, we'll need to actually examine the file.
				}
			}
			return null
		}
	}
}