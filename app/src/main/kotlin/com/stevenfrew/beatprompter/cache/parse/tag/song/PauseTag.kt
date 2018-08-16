package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

class PauseTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): Tag(name,lineNumber,position) {
    val mDuration:Long = parseDurationValue(value, 1000L, 60 * 60 * 1000L, false)
}