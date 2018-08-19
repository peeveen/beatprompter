package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

class TagTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): ValueTag(name,lineNumber,position,value) {
    val mTag=value
}