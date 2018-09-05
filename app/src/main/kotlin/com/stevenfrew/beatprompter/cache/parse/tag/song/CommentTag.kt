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
class CommentTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): ValueTag(name,lineNumber,position,value) {
    val mAudience:List<String>
    val mComment:String
    init {
        val bits=value.splitAndTrim(AUDIENCE_END_MARKER)
        if(bits.size>1) {
            mAudience = bits[0].splitAndTrim(AUDIENCE_SEPARATOR)
            mComment = bits[1]
        }
        else {
            mAudience=listOf()
            mComment=value
        }
    }
    companion object {
        const val AUDIENCE_END_MARKER="|||||"
        const val AUDIENCE_SEPARATOR="@"
    }
}


