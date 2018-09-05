package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.midi.*
import com.stevenfrew.beatprompter.splitAndTrim

/**
 * Base class for MIDI auto-start tags.
 */
open class MIDITriggerTag protected constructor(name:String,lineNumber:Int,position:Int,triggerDescriptor:String,type: TriggerType): Tag(name,lineNumber,position) {
    val mTrigger:SongTrigger

    init {
        val bits = triggerDescriptor.splitAndTrim(",")
        var msb: Value = WildcardValue()
        var lsb: Value = WildcardValue()
        var channel: Value = WildcardValue()
        if (bits.size > 1)
            if (type==TriggerType.SongSelect)
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.song_index_must_have_one_value))
        if (bits.size > 4 || bits.isEmpty())
            if (type==TriggerType.SongSelect)
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.song_index_must_have_one_value))
            else
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.song_index_must_have_one_two_or_three_values))

        if (bits.size > 3) {
            val value = Tag.parseMIDIValue(bits[3], 3, bits.size)
            if (value is ChannelValue)
                channel = value
        }

        if (bits.size > 2)
            lsb = Tag.parseMIDIValue(bits[2], 2, bits.size)

        if (bits.size > 1)
            msb = Tag.parseMIDIValue(bits[1], 1, bits.size)

        val index = Tag.parseMIDIValue(bits[0], 0, bits.size)

        mTrigger=SongTrigger(msb, lsb, index, channel, type)
    }
}