package com.stevenfrew.beatprompter.cache.parse.tag.set

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

class SetNameTag internal constructor(name:String, lineNumber:Int, position:Int, value:String): Tag(name,lineNumber,position) {
    val mSetName=value
}