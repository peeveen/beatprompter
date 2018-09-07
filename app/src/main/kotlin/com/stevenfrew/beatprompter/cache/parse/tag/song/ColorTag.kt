package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.Utils
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.TagUtility
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

/**
 * Base class for tags that define a color for some purpose.
 */
open class ColorTag protected constructor(name:String,lineNumber:Int,position:Int,value:String): ValueTag(name,lineNumber,position,value) {
    val mColor:Int = Utils.makeHighlightColour(TagUtility.parseColourValue(value))
}