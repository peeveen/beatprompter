package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.StartedBy
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@StartedBy(StartOfHighlightTag::class)
class EndOfHighlightTag internal constructor(name:String,lineNumber:Int,position:Int): Tag(name,lineNumber,position)