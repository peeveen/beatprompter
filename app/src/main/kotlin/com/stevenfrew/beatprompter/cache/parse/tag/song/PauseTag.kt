package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.*
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@TagName("pause")
@TagType(Type.Directive)
/**
 * Tag that defines a pause that should occur at this point in the song.
 */
class PauseTag internal constructor(name: String,
                                    lineNumber: Int,
                                    position: Int,
                                    value: String)
    : ValueTag(name, lineNumber, position, value) {
    val mDuration = TagParsingUtility.parseDurationValue(value,
            1000L, 60 * 60 * 1000L, false)
}