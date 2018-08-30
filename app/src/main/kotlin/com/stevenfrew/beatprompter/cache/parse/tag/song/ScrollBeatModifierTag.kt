package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@NormalizedName("<>")
class ScrollBeatModifierTag internal constructor(name:String, lineNumber:Int, position:Int): Tag(name,lineNumber,position) {
    val mModifier=if(name=="<") -1 else 1
}