package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.*

@OncePerLine
@StartedBy(BeatStartTag::class)
@LineExclusive(BeatStartTag::class)
@TagName("beatstop")
/**
 * Tag that means "switch to manual mode" on this line.
 */
class BeatStopTag internal constructor(name:String,lineNumber:Int,position:Int): Tag(name,lineNumber,position)