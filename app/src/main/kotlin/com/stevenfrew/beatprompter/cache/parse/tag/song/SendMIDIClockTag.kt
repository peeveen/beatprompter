package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@OncePerFile
@NormalizedName("send_midi_clock")
class SendMIDIClockTag internal constructor(name:String, lineNumber:Int, position:Int): Tag(name,lineNumber,position)