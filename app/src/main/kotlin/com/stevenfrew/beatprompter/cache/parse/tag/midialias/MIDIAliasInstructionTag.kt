package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag

class MIDIAliasInstructionTag internal constructor(name:String,lineNumber:Int,position:Int,val mInstructions:String): MIDITag(name,lineNumber,position,mInstructions)