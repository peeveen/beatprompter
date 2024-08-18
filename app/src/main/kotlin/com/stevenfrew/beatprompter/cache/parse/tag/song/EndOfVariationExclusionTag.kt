package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.StartedBy
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@StartedBy(StartOfVariationExclusionTag::class)
@TagName("varxend", "varexend", "varxstop", "varexstop", "end_of_variation_exclusion")
@TagType(Type.Directive)
@OncePerLine
/**
 * Tag that defines the end of a variation exclusion section.
 */
class EndOfVariationExclusionTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int
) : Tag(name, lineNumber, position)