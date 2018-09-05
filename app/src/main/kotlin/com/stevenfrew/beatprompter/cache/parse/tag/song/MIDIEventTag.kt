package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.SongList
import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.event.MIDIEvent
import com.stevenfrew.beatprompter.midi.EventOffset
import com.stevenfrew.beatprompter.midi.OutgoingMessage

@TagType(Type.Directive)
/**
 * Tag that defines a MIDI event to be output to any connected devices.
 */
class MIDIEventTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): MIDITag(name,lineNumber,position) {
    val mMessages:List<OutgoingMessage>
    val mOffset: EventOffset?
    init {
        val parsedEvent=parseMIDIEvent(name,value,SongList.mCachedCloudFiles.midiAliases)
        mMessages=parsedEvent.first
        mOffset=parsedEvent.second
    }
    fun toMIDIEvent(time:Long): MIDIEvent
    {
        return MIDIEvent(time,mMessages,mOffset)
    }
}
