package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@TagName("b","bars")
@TagType(Type.Directive)
/**
 * Tag that defines how many bars the current line lasts for.
 */
class BarsTag internal constructor(name:String, lineNumber:Int, position:Int, value:String): ValueTag(name,lineNumber,position,value) {
    val mBars:Int = parseIntegerValue(value, 1, 128)
}