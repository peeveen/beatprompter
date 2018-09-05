package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.SongList
import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag

/**
 * Tag that defines a MIDI event to be output to any connected devices.
 */
class MIDIEventTag internal constructor(name:String,lineNumber:Int,position:Int,value:String,time:Long,defaultChannel:Byte): MIDITag(name,lineNumber,position,value) {
    val mEvent=parseMIDIEvent(value,time,SongList.mCachedCloudFiles.midiAliases,defaultChannel)
}
