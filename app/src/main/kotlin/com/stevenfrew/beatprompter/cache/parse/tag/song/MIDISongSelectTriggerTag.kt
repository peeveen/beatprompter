package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.midi.TriggerType

@OncePerFile
@NormalizedName("midi_song_select")
/**
 * Tag that defines a MIDI song select event that, if received, will cause this song to be
 * automatically started.
 */
class MIDISongSelectTriggerTag internal constructor(name:String,lineNumber:Int,position:Int,triggerDescriptor:String): MIDITriggerTag(name,lineNumber,position,triggerDescriptor, TriggerType.SongSelect)
