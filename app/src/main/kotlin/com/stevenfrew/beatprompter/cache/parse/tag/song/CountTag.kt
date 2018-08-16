package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

class CountTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): Tag(name,lineNumber,position) {
    val mCount:Int=parseIntegerValue(value,0,4)
}
