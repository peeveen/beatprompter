package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.splitAndTrim

@OncePerLine
@TagName("comment", "c", "comment_box", "cb", "comment_italic", "ci")
@TagType(Type.Directive)
/**
 * Tag that defines a comment that is to be shown on the song title screen, or during playback.
 */
class CommentTag internal constructor(name:String,lineNumber:Int,position:Int,val mComment:String): ValueTag(name,lineNumber,position,mComment) {
    val mAudience:List<String> = if(mName.contains('@'))
        mName.substringAfter('@').splitAndTrim("@")
    else
        listOf()
}


