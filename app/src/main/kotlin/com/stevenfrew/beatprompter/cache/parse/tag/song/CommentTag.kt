package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

class CommentTag internal constructor(name:String,lineNumber:Int,position:Int,val mComment:String): Tag(name,lineNumber,position) {
    val mAudience:List<String> = if(mName.contains('@'))
        mName.substringAfter('@').split("@").map{it.trim()}
    else
        listOf()

}


