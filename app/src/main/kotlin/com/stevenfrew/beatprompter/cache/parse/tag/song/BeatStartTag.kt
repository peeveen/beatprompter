package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.*

@OncePerLine
@EndedBy(BeatStopTag::class)
@LineExclusive(BeatStopTag::class)
@NormalizedName("beatstart")
class BeatStartTag internal constructor(name:String,lineNumber:Int,position:Int): Tag(name,lineNumber,position)