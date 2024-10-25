package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.StartedBy
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@StartedBy(StartOfVariationInclusionTag::class)
@TagName("varend", "varstop", "end_of_variation", "endofvariation")
@TagType(Type.Directive)
@OncePerLine
/**
 * Tag that defines the end of a variation inclusion section.
 */
class EndOfVariationInclusionTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int
) : Tag(name, lineNumber, position)