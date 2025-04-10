package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.StartedBy
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@StartedBy(StartOfChorusTag::class)
@TagName("eoc", "endofchorus", "end_of_chorus")
@TagType(Type.Directive)
/**
 * Tag that defines the end of a highlighted block of text.
 */
class EndOfChorusTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int
) : Tag(name, lineNumber, position)