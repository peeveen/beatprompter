package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

@OncePerLine
class CommentTag internal constructor(name:String,lineNumber:Int,position:Int,val mComment:String): ValueTag(name,lineNumber,position,mComment) {
    val mAudience:List<String> = if(mName.contains('@'))
        mName.substringAfter('@').split("@").map{it.trim()}
    else
        listOf()
}


