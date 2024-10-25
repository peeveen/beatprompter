package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.EndedBy
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@EndedBy(EndOfChorusTag::class)
@TagName("soc", "start_of_chorus", "startofchorus")
@TagType(Type.Directive)
/**
 * Tag that defines the start of a block of highlighted text.
 */
class StartOfChorusTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int
) : Tag(
	name,
	lineNumber,
	position
)