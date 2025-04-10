package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.util.normalize
import com.stevenfrew.beatprompter.util.splitAndTrim
import java.io.File

@TagName("audio", "track", "musicpath")
@TagType(Type.Directive)
/**
 * Tag that describes an accompanying audio file for a song file.
 */
class AudioTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : ValueTag(name, lineNumber, position, value) {
	val filename: String
	val normalizedFilename: String
	val volume: Int

	init {
		val bits = value.splitAndTrim(":")
		val defaultTrackVolume = BeatPrompter.preferences.defaultTrackVolume
		filename = File(bits[0]).name
		normalizedFilename = filename.normalize()
		volume =
			if (bits.size > 1)
				parseVolume(bits[1], defaultTrackVolume)
			else
				defaultTrackVolume
	}

	companion object {
		fun parseVolume(value: String, defaultTrackVolume: Int): Int =
			try {
				val absolute = value.startsWith('=')
				val factor = if (absolute) 100.0 else defaultTrackVolume.toDouble()
				value.trim('=').toDouble().takeIf { it in 0.0..100.0 }?.let {
					(factor * (it / 100.0)).toInt()
				} ?: throw MalformedTagException(R.string.badAudioVolume)
			} catch (_: NumberFormatException) {
				throw MalformedTagException(R.string.badAudioVolume)
			}
	}
}
