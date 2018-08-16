package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag

class MIDIAliasInstructionTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): MIDITag(name,lineNumber,position) {
    val mInstructions=value
}