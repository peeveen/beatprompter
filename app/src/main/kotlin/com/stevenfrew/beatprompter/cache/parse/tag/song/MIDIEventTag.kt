package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.SongList
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.event.MIDIEvent
import com.stevenfrew.beatprompter.midi.*
import com.stevenfrew.beatprompter.splitAndTrim

@TagType(Type.Directive)
/**
 * Tag that defines a MIDI event to be output to any connected devices.
 */
class MIDIEventTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): Tag(name,lineNumber,position) {
    val mMessages:List<OutgoingMessage>
    val mOffset: EventOffset?
    init {
        val parsedEvent=parseMIDIEvent(name,value,lineNumber,SongList.mCachedCloudFiles.midiAliases)
        mMessages=parsedEvent.first
        mOffset=parsedEvent.second
    }
    fun toMIDIEvent(time:Long): MIDIEvent
    {
        return MIDIEvent(time,mMessages,mOffset)
    }
    companion object {
        @Throws(MalformedTagException::class)
        fun parseMIDIEvent(name:String,value:String, lineNumber:Int,aliases: List<Alias>): Pair<List<OutgoingMessage>,EventOffset?> {

            val defaultChannelPref= BeatPrompterApplication.preferences.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_default)))
            val defaultChannel= Message.getChannelFromBitmask(defaultChannelPref)

            var tagName=name
            var tagValue = value
            var eventOffset: EventOffset? = null
            if (tagValue.isEmpty()) {
                // A MIDI tag of {blah;+33} ends up with "blah;+33" as the tag name. Fix it here.
                if (tagName.contains(";")) {
                    val bits = tagName.splitAndTrim(";")
                    if (bits.size > 2)
                        throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.multiple_semi_colons_in_midi_tag))
                    if (bits.size > 1) {
                        eventOffset = parseMIDIEventOffset(bits[1],lineNumber)
                        tagName = bits[0]
                    }
                }
            } else {
                val firstSplitBits = tagValue.splitAndTrim(";")
                if (firstSplitBits.size > 1) {
                    if (firstSplitBits.size > 2)
                        throw MalformedTagException( BeatPrompterApplication.getResourceString(R.string.multiple_semi_colons_in_midi_tag))
                    tagValue = firstSplitBits[0]
                    eventOffset = parseMIDIEventOffset(firstSplitBits[1],lineNumber)
                }
            }
            var paramValues = tagValue.splitAndTrim(",").filter{!it.isBlank()}.map { bit -> parseValue(bit) }
            var lastParamIsChannel = false
            var channel = defaultChannel
            for (f in paramValues.indices)
                if (paramValues[f] is ChannelValue)
                    if (f == paramValues.size - 1)
                        lastParamIsChannel = true
                    else
                        throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.channel_must_be_last_parameter))
            try {
                if (lastParamIsChannel) {
                    val lastParam = paramValues[paramValues.size - 1] as ChannelValue
                    channel = lastParam.resolve()
                    paramValues = paramValues.dropLast(1)
                }
                val resolvedBytes = ByteArray(paramValues.size)
                for (f in paramValues.indices)
                    resolvedBytes[f] = paramValues[f].resolve()
                for (alias in aliases)
                    if (alias.mName.equals(tagName, ignoreCase = true))
                        return Pair(alias.resolve(aliases, resolvedBytes, channel), eventOffset)
                if (tagName == "midi_send")
                    return Pair(listOf(OutgoingMessage(resolvedBytes)), eventOffset)
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.unknown_midi_directive, tagName))
            } catch (re: ResolutionException) {
                throw MalformedTagException(re.message!!)
            }
        }

        @Throws(MalformedTagException::class)
        fun parseMIDIEventOffset(offsetString:String,lineNumber:Int):EventOffset
        {
            var amount=0
            var offsetType: EventOffsetType = EventOffsetType.Milliseconds
            var str = offsetString
            str = str.trim()
            if (!str.isEmpty()) {
                try {
                    amount = Integer.parseInt(str)
                    offsetType = EventOffsetType.Milliseconds
                } catch (e: Exception) {
                    // Might be in the beat format
                    var diff = 0
                    var bErrorAdded = false
                    str.toCharArray().forEach{
                        if (it == '<')
                            --diff
                        else if (it == '>')
                            ++diff
                        else if (!bErrorAdded) {
                            bErrorAdded = true
                            throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.non_beat_characters_in_midi_offset))
                        }
                    }
                    amount = diff
                    offsetType = EventOffsetType.Beats
                }

                if (Math.abs(amount) > 16 && offsetType == EventOffsetType.Beats)
                    throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded))
                else if (Math.abs(amount) > 10000 && offsetType == EventOffsetType.Milliseconds)
                    throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded))
            }
            return EventOffset(amount,offsetType,lineNumber)
        }

        @Throws(MalformedTagException::class)
        fun parseValue(strVal: String): Value {
            return TagParsingUtility.parseMIDIValue(strVal, 0, 1)
        }
    }
}
