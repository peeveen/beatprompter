package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.SongList
import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag

class MIDIEventTag internal constructor(name:String,lineNumber:Int,position:Int,value:String,time:Long,defaultChannel:Byte): MIDITag(name,lineNumber,position) {
    val mEvent=parseMIDIEvent(value,time,SongList.mCachedCloudFiles.midiAliases,defaultChannel)
}
