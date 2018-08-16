package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

class TitleTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): Tag(name,lineNumber,position) {
    val mTitle=value
}