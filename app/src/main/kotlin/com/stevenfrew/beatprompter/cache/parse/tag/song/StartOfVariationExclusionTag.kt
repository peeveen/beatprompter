package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.EndedBy
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@EndedBy(EndOfVariationExclusionTag::class)
@TagName("varxstart", "varexstart", "start_of_variation_exclusion", "startofvariationexclusion")
@TagType(Type.Directive)
@OncePerLine
/**
 * Tag that defines the start of a variation exclusion section
 */
class StartOfVariationExclusionTag internal constructor(
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