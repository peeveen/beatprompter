package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@TagName("<", ">")
@TagType(Type.Shorthand)
/**
 * Shorthand tag that can increase/reduce the current scrollbeat.
 */
class ScrollBeatModifierTag internal constructor(name: String,
                                                 lineNumber: Int,
                                                 position: Int)
    : Tag(name, lineNumber, position) {
    val mModifier = if (name == "<") -1 else 1
}