package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.*
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("time")
@TagType(Type.Directive)
/**
 * Tag that defines how long this song should take to scroll from top to bottom in smooth mode.
 */
class TimeTag internal constructor(name: String,
                                   lineNumber: Int,
                                   position: Int,
                                   value: String)
    : ValueTag(name, lineNumber, position, value) {
    val mDuration = TagParsingUtility.parseDurationValue(value,
            1000L, 60 * 60 * 1000L, true)
}
