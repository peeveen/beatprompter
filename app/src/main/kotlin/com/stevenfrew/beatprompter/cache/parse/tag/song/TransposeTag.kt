package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@TagName("transpose")
@TagType(Type.Directive)
/**
 * Transposes the chord map by the given amount.
 */
class TransposeTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	val value: String
) : Tag(name, lineNumber, position)