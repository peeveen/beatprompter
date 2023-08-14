package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.*
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@TagName("scrollbeat")
@TagType(Type.Directive)
/**
 * Tag that defines (or redefines) the scrollbeat that should be used from this line onwards.
 */
class ScrollBeatTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : ValueTag(name, lineNumber, position, value) {
	val mScrollBeat = TagParsingUtility.parseIntegerValue(value, 1, 32)
}