package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.StartedBy
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@OncePerLine
@StartedBy(BeatStartTag::class)
class BeatStopTag internal constructor(name:String,lineNumber:Int,position:Int): Tag(name,lineNumber,position)