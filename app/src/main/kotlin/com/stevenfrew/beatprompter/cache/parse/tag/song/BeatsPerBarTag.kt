package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@TagName("bpb", "beatsperbar")
@TagType(Type.Directive)
/**
 * Tag that defines (or redefines) how many beats there are in each bar of a song file from
 * this point onwards.
 */
class BeatsPerBarTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : ValueTag(name, lineNumber, position, value) {
	val bpb = TagParsingUtility.parseIntegerValue(value, 1, 32)
}

