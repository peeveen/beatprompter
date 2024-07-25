package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("key")
@TagType(Type.Directive)
/**
 * Tag that defines what key the current song is in.
 */
class KeyTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	val key: String
) : ValueTag(name, lineNumber, position, key)