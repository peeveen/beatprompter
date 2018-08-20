package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.midi.TriggerType

@OncePerFile
@NormalizedName("midi_program_change")
class MIDIProgramChangeTriggerTag internal constructor(name:String, lineNumber:Int, position:Int, triggerDescriptor:String): MIDITriggerTag(name,lineNumber,position,triggerDescriptor, TriggerType.ProgramChange)
