package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

@OncePerLine
@TagName("bpl","barsperline")
/**
 * Tag that defines (or redefines) how many bars there are in each line of a song file from
 * this point onwards.
 */
class BarsPerLineTag internal constructor(name:String, lineNumber:Int, position:Int, value:String): ValueTag(name,lineNumber,position,value) {
    val mBPL:Int = parseIntegerValue(value, 1,32)
}