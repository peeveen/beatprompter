package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

@OncePerFile
@TagName("time")
/**
 * Tag that defines how long this song should take to scroll from top to bottom in smooth mode.
 */
class TimeTag internal constructor(name:String, lineNumber:Int, position:Int, value:String): ValueTag(name,lineNumber,position,value) {
    val mDuration:Long = parseDurationValue(value, 1000L, 60 * 60 * 1000L, true)
}
