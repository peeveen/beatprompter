package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag
import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile

@OncePerFile
@NormalizedName("midialiases")
class MIDIAliasSetNameTag internal constructor(name:String, lineNumber:Int, position:Int, val mAliasSetName:String): MIDITag(name,lineNumber,position,mAliasSetName)