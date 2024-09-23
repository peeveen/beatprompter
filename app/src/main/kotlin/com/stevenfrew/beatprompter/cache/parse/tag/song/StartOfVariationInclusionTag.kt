package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.EndedBy
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

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
) : StartOfVariationTag(
	name,
	lineNumber,
	position,
	value
)