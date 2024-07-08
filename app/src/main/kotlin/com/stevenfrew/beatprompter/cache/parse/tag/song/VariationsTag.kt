package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.util.splitAndTrim
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("variations")
@TagType(Type.Directive)
/**
 * Tag that defines the names of variations of a song
 */
class VariationsTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : Tag(name, lineNumber, position) {
	val mVariations: List<String> = value.splitAndTrim(",").filter { it.isNotBlank() }
}