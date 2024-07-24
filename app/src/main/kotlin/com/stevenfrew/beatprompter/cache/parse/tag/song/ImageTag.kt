package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.graphics.ImageScalingMode
import com.stevenfrew.beatprompter.util.splitAndTrim
import java.io.File
import java.util.Locale

@OncePerLine
@TagName("image")
@TagType(Type.Directive)
/**
 * Tag that defines an image to use for the current line instead of text.
 */
class ImageTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : ValueTag(name, lineNumber, position, value) {
	val filename: String
	val scalingMode: ImageScalingMode

	init {
		val bits = value.splitAndTrim(":")
		filename = File(bits[0]).name
		scalingMode =
			if (bits.size > 1)
				parseImageScalingMode(bits[1])
			else
				ImageScalingMode.Stretch
	}

	companion object {
		fun parseImageScalingMode(value: String): ImageScalingMode =
			try {
				val locale = Locale.getDefault()
				ImageScalingMode.valueOf(value.lowercase()
					.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() })
			} catch (e: Exception) {
				throw MalformedTagException(R.string.unknown_image_scaling_mode)
			}
	}
}
