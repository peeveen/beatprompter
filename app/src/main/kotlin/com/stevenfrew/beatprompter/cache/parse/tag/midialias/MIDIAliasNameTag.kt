package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag
import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine

@OncePerLine
@NormalizedName("midialias")
class MIDIAliasNameTag internal constructor(name:String, lineNumber:Int, position:Int, val mAliasName:String): MIDITag(name,lineNumber,position,mAliasName)