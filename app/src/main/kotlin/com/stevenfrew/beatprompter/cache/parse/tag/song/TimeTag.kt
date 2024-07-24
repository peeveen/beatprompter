package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("time")
@TagType(Type.Directive)
/**
 * Tag that defines how long this song should take to scroll from top to bottom in smooth mode.
 */
class TimeTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : ValueTag(name, lineNumber, position, value) {
	val duration = TagParsingUtility.parseDurationValue(
		value,
		1000L, 60 * 60 * 1000L, true
	)
}
