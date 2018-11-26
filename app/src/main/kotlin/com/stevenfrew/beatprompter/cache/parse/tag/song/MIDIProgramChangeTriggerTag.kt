package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.midi.TriggerType

@OncePerFile
@TagName("midi_program_change_trigger")
@TagType(Type.Directive)
/**
 * Tag that defines a MIDI program change event that, if received, will cause this song to be
 * automatically started.
 */
class MIDIProgramChangeTriggerTag internal constructor(name: String, lineNumber: Int, position: Int, triggerDescriptor: String) : MIDITriggerTag(name, lineNumber, position, triggerDescriptor, TriggerType.ProgramChange)
