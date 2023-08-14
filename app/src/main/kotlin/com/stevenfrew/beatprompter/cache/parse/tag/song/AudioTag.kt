package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.Preferences
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
	val mFilename: String
	val mVolume: Int

	init {
		val bits = value.splitAndTrim(":")
		val defaultTrackVolume = Preferences.defaultTrackVolume
		mFilename = File(bits[0]).name.normalize()
		mVolume =
			if (bits.size > 1)
				parseVolume(bits[1], defaultTrackVolume)
			else
				defaultTrackVolume
	}

	companion object {
		fun parseVolume(value: String, defaultTrackVolume: Int): Int {
			try {
				val tryVolume = value.toInt()
				if (tryVolume in 0..100)
					return (defaultTrackVolume.toDouble() * (tryVolume.toDouble() / 100.0)).toInt()
				throw MalformedTagException(R.string.badAudioVolume)
			} catch (nfe: NumberFormatException) {
				throw MalformedTagException(R.string.badAudioVolume)
			}
		}
	}
}