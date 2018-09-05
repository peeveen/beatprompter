package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.*
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("midi_aliases")
@TagType(Type.Directive)
/**
 * Tag that defines a MIDI alias set name.
 */
class MIDIAliasSetNameTag internal constructor(name:String, lineNumber:Int, position:Int, val mAliasSetName:String): Tag(name,lineNumber,position)