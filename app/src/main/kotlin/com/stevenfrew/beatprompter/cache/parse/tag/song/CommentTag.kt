package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.util.splitAndTrim

@OncePerLine
@TagName("comment", "c", "comment_box", "cb", "comment_italic", "ci")
@TagType(Type.Directive)
/**
 * Tag that defines a comment that is to be shown on the song title screen, or during playback.
 */
class CommentTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : ValueTag(name, lineNumber, position, value) {
	val audience: List<String>
	val comment: String
	val color: Int?

	init {
		val colorBits = value.splitAndTrim(COLOR_SEPARATOR)
		color = if (colorBits.size > 1)
			TagParsingUtility.parseColourValue(colorBits[1])
		else
			null
		val audienceBits = colorBits[0].splitAndTrim(AUDIENCE_END_MARKER)
		if (audienceBits.size > 1) {
			audience = audienceBits[0].splitAndTrim(AUDIENCE_SEPARATOR)
			comment = audienceBits[1]
		} else {
			audience = listOf()
			comment = colorBits[0]
		}
	}

	companion object {
		const val AUDIENCE_END_MARKER = "|||||"
		const val AUDIENCE_SEPARATOR = "@"
		const val COLOR_SEPARATOR = "###"
	}
}


