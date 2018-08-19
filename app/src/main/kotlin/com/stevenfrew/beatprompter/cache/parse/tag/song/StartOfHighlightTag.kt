package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.EndedBy

@EndedBy(EndOfHighlightTag::class)
class StartOfHighlightTag internal constructor(name:String,lineNumber:Int,position:Int,value:String,defaultHighlightColour:Int)
    : ColorTag(name,lineNumber,position,if(value.isBlank()) "#"+(defaultHighlightColour.toString(16).padStart(6,'0')) else value)