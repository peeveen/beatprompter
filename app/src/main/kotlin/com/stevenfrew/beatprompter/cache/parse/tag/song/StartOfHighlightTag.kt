package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.EndedBy
import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName

@EndedBy(EndOfHighlightTag::class)
@NormalizedName("soh")
/**
 * Tag that defines the start of a block of highlighted text.
 */
class StartOfHighlightTag internal constructor(name:String,lineNumber:Int,position:Int,value:String,defaultHighlightColour:Int)
    : ColorTag(name,lineNumber,position,if(value.isBlank()) "#"+((defaultHighlightColour and 0x00FFFFFF).toString(16).padStart(6,'0')) else value)