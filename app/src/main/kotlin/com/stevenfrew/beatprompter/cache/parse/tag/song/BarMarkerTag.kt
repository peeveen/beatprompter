package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@TagName(",")
@TagType(Type.Shorthand)
/**
 * Shorthand tag that means "one bar". Multiples of these indicate how many bars the current
 * song file line lasts for.
 */
class BarMarkerTag internal constructor(name: String, lineNumber: Int, position: Int) : Tag(name, lineNumber, position)