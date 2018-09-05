package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("midi_aliases")
@TagType(Type.Directive)
/**
 * Tag that defines a MIDI alias set name.
 */
class MIDIAliasSetNameTag internal constructor(name:String, lineNumber:Int, position:Int, val mAliasSetName:String): MIDITag(name,lineNumber,position)