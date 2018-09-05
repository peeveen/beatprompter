package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@OncePerFile
@TagName("send_midi_clock")
/**
 * Tag that instructs the app to output MIDI clock signals at the same tempo as the song.
 */
class SendMIDIClockTag internal constructor(name:String, lineNumber:Int, position:Int): Tag(name,lineNumber,position)