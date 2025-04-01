package com.stevenfrew.beatprompter.cache.parse.tag.midi.trigger

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.midi.TriggerType

@OncePerLine
@TagName("midi_control_change_trigger", "midicontrolchangetrigger")
@TagType(Type.Directive)
/**
 * Tag that defines a MIDI program change event that, if received, will cause this song to be
 * automatically started.
 */
class MidiControlChangeTriggerTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	triggerDescriptor: String
) : MidiTriggerTag(name, lineNumber, position, triggerDescriptor, TriggerType.ControlChange)
