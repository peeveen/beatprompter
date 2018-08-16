package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

class BeatsPerMinuteTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): Tag(name,lineNumber,position) {
    val mBPM:Double = parseDoubleValue(value, 10.0,300.0)
}