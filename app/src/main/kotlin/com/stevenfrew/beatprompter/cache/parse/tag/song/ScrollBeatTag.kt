package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

@OncePerLine
@TagName("scrollbeat")
/**
 * Tag that defines (or redefines) the scrollbeat that should be used from this line onwards.
 */
class ScrollBeatTag internal constructor(name:String, lineNumber:Int, position:Int, value:String): ValueTag(name,lineNumber,position,value) {
    val mScrollBeat:Int = parseIntegerValue(value, 1, 32)
}