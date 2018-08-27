package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.splitAndTrim

@OncePerLine
@NormalizedName("c")
class CommentTag internal constructor(name:String,lineNumber:Int,position:Int,val mComment:String): ValueTag(name,lineNumber,position,mComment) {
    val mAudience:List<String> = if(mName.contains('@'))
        mName.substringAfter('@').splitAndTrim("@")
    else
        listOf()
}


