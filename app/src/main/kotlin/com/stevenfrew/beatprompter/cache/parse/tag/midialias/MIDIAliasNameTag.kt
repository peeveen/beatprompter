package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine

@OncePerLine
@TagName("midialias")
/**
 * Tag that defines a MIDI alias name.
 */
class MIDIAliasNameTag internal constructor(name:String, lineNumber:Int, position:Int, val mAliasName:String): MIDITag(name,lineNumber,position,mAliasName)