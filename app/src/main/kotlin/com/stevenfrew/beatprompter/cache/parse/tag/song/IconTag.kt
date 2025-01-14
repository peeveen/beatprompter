package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("icon", "i")
@TagType(Type.Directive)
/**
 * Tag that defines an icon for the song.
 */
class IconTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	val icon: String
) : ValueTag(name, lineNumber, position, icon)