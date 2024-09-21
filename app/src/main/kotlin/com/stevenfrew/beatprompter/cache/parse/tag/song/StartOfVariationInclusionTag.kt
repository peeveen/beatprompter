package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.EndedBy
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.util.splitAndTrim

@EndedBy(EndOfVariationInclusionTag::class)
@TagName("varstart", "start_of_variation")
@TagType(Type.Directive)
@OncePerLine
/**
 * Tag that defines the start of a variation inclusion section
 */
class StartOfVariationInclusionTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : Tag(
	name,
	lineNumber,
	position,
) {
	val variations: List<String> = value.splitAndTrim(",").filter { it.isNotBlank() }
}