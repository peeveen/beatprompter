package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.midi.TriggerType

class MIDIProgramChangeTriggerTag internal constructor(name:String,lineNumber:Int,position:Int,triggerDescriptor:String): MIDITriggerTag(name,lineNumber,position,triggerDescriptor, TriggerType.ProgramChange)
