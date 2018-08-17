package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag

class MIDIAliasSetNameTag internal constructor(name:String, lineNumber:Int, position:Int, value:String): MIDITag(name,lineNumber,position) {
    val mAliasSetName=value
}