package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile

@OncePerFile
@TagName("midialiases")
/**
 * Tag that defines a MIDI alias set name.
 */
class MIDIAliasSetNameTag internal constructor(name:String, lineNumber:Int, position:Int, val mAliasSetName:String): MIDITag(name,lineNumber,position,mAliasSetName)