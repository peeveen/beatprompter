package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.EndedBy
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@OncePerLine
@EndedBy(BeatStopTag::class)
class BeatStartTag internal constructor(name:String,lineNumber:Int,position:Int): Tag(name,lineNumber,position)