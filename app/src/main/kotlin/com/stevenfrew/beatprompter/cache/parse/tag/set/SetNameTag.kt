package com.stevenfrew.beatprompter.cache.parse.tag.set

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("set")
@TagType(Type.Directive)
/**
 * Tag that defines the name of a setlist.
 */
class SetNameTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	val setName: String
) : ValueTag(name, lineNumber, position, setName)