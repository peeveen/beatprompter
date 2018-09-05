package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@TagName("<>")
/**
 * Shorthand tag that can increase/reduce the current scrollbeat.
 */
class ScrollBeatModifierTag internal constructor(name:String, lineNumber:Int, position:Int): Tag(name,lineNumber,position) {
    val mModifier=if(name=="<") -1 else 1
}