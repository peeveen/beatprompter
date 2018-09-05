package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("count","countin")
@TagType(Type.Directive)
/**
 * Tag that defines how many bars of "count-in" are played before the song properly starts.
 */
class CountTag internal constructor(name:String, lineNumber:Int, position:Int, value:String): ValueTag(name,lineNumber,position,value) {
    val mCount:Int=parseIntegerValue(value,0,4)
}
