package com.stevenfrew.beatprompter.cache.parse.tag.midi.trigger

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.midi.TriggerType

@OncePerFile
@TagName("midi_song_select_trigger", "midisongselecttrigger")
@TagType(Type.Directive)
/**
 * Tag that defines a MIDI song select event that, if received, will cause this song to be
 * automatically started.
 */
class MidiSongSelectTriggerTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	triggerDescriptor: String
) : MidiTriggerTag(name, lineNumber, position, triggerDescriptor, TriggerType.SongSelect)
