package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.EndedBy
import com.stevenfrew.beatprompter.cache.parse.tag.LineExclusive
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@EndedBy(BeatStopTag::class)
@LineExclusive(BeatStopTag::class)
@TagName("beatstart")
@TagType(Type.Directive)
/**
 * Tag that means "switch to beat mode" on this line.
 */
class BeatStartTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int
) : Tag(name, lineNumber, position)