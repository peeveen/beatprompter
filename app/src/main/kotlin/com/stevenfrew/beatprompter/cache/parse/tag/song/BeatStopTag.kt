package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.LineExclusive
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.StartedBy
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@StartedBy(BeatStartTag::class)
@LineExclusive(BeatStartTag::class)
@TagName("beatstop")
@TagType(Type.Directive)
/**
 * Tag that means "switch to manual mode" on this line.
 */
class BeatStopTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int
) : Tag(name, lineNumber, position)