package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Utils
import com.stevenfrew.beatprompter.event.MIDIEvent
import com.stevenfrew.beatprompter.midi.*
import java.util.ArrayList
import kotlin.experimental.and

open class MIDITag protected constructor(name:String,lineNumber:Int,position:Int): Tag(name,lineNumber,position) {
    @Throws(MalformedTagException::class)
    fun parseMIDIEvent(value:String, time: Long, aliases: List<Alias>, defaultChannel: Byte): MIDIEvent {
        var tagValue = value.trim()
        var name=mName
        var eventOffset: EventOffset = EventOffset.NoOffset
        if (tagValue.isEmpty()) {
            // A MIDI tag of {blah;+33} ends up with "blah;+33" as the tag name. Fix it here.
            if (name.contains(";")) {
                val bits = name.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (bits.size > 2)
                    throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.multiple_semi_colons_in_midi_tag))
                if (bits.size > 1) {
                    eventOffset = parseMIDIEventOffset(bits[1].trim())
                    name = bits[0].trim()
                }
            }
        } else {
            val firstSplitBits = tagValue.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (firstSplitBits.size > 1) {
                if (firstSplitBits.size > 2)
                    throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.multiple_semi_colons_in_midi_tag))
                tagValue = firstSplitBits[0].trim()
                eventOffset = parseMIDIEventOffset(firstSplitBits[1].trim())
            }
        }
        val bits = if (tagValue.isEmpty()) arrayOf() else tagValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var paramBytes = bits.map { bit -> parseValue(bit.trim()) }
        var lastParamIsChannel = false
        var channel = defaultChannel
        for (f in paramBytes.indices)
            if (paramBytes[f] is ChannelValue)
                if (f == paramBytes.size - 1)
                    lastParamIsChannel = true
                else
                    throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.channel_must_be_last_parameter))
        try {
            if (lastParamIsChannel) {
                val lastParam = paramBytes[paramBytes.size - 1] as ChannelValue
                channel = lastParam.resolve()
                val paramBytesWithoutChannel = paramBytes.subList(0, paramBytes.size - 1)
                paramBytes = paramBytesWithoutChannel
            }
            val resolvedBytes = ByteArray(paramBytes.size)
            for (f in paramBytes.indices)
                resolvedBytes[f] = paramBytes[f].resolve()
            for (alias in aliases)
                if (alias.mName.equals(name, ignoreCase = true)) {
                    return MIDIEvent(time, alias.resolve(aliases, resolvedBytes, channel), eventOffset)
                }
            if (name == "midi_send")
                return MIDIEvent(time, OutgoingMessage(resolvedBytes), eventOffset)
            throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.unknown_midi_directive, name))
        } catch (re: ResolutionException) {
            throw MalformedTagException(this, re)
        }
    }

    @Throws(MalformedTagException::class)
    fun parseMIDIEventOffset(offsetString:String):EventOffset
    {
        var amount=0
        var offsetType:EventOffsetType=EventOffsetType.Milliseconds
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
                        throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.non_beat_characters_in_midi_offset))
                    }
                }
                amount = diff
                offsetType = EventOffsetType.Beats
            }

            if (Math.abs(amount) > 16 && offsetType == EventOffsetType.Beats)
                throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded))
            else if (Math.abs(amount) > 10000 && offsetType == EventOffsetType.Milliseconds)
                throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded))
        }
        return EventOffset(amount,offsetType,this)
    }

    @Throws(MalformedTagException::class)
    fun parseAliasComponent(line: String): AliasComponent? {
        val bracketStart = line.indexOf("{")
        if (bracketStart != -1) {
            val bracketEnd = line.indexOf("}", bracketStart)
            if (bracketEnd != -1) {
                val contents = line.substring(bracketStart + 1, bracketEnd - bracketStart).trim().toLowerCase()
                if (contents.isNotEmpty()) {
                    val bits = contents.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val componentArgs = ArrayList<Value>()
                    val tagName = bits[0].trim()
                    if (bits.size > 2)
                        throw MalformedTagException(this,BeatPrompterApplication.getResourceString(R.string.midi_alias_message_contains_more_than_two_parts))
                    else if (bits.size > 1) {
                        val params = bits[1].trim()
                        val paramBits = params.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        for ((paramCounter, paramBit) in paramBits.withIndex()) {
                            val aliasValue = parseValue(paramBit, paramCounter, paramBits.size,this)
                            componentArgs.add(aliasValue)
                        }
                    }

                    return if (tagName.equals("midi_send", ignoreCase = true))
                        SimpleAliasComponent(componentArgs)
                    else
                        RecursiveAliasComponent(tagName, componentArgs)
                } else
                    throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.empty_tag))
            } else
                throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.badly_formed_tag))
        } else
            throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.badly_formed_tag))
    }

    @Throws(MalformedTagException::class)
    fun parseValue(strVal: String): Value {
        return parseValue(strVal, 0, 1,this)
    }

    @Throws(MalformedTagException::class)
    fun parseChannelValue(channelValStr: String): Value {
        return parseChannelValue(channelValStr,this)
    }

    @Throws(MalformedTagException::class)
    fun parseValue(valueStr: String, argIndex: Int, argCount: Int): Value {
        return parseValue(valueStr,argIndex,argCount,this)
    }

    companion object {
        @Throws(MalformedTagException::class)
        fun parseValue(strVal: String): Value {
            return parseValue(strVal, 0, 1,null)
        }

        @Throws(MalformedTagException::class)
        fun parseChannelValue(channelValStr: String,tag:MIDITag?=null): Value {
            var strVal = channelValStr
            if (!strVal.isEmpty())
                if (!strVal.startsWith("*"))
                    if (!strVal.startsWith("#"))
                        strVal = "#$strVal"
            return parseValue(strVal,0,1,tag)
        }

        @Throws(MalformedTagException::class)
        fun parseValue(valueStr: String, argIndex: Int, argCount: Int, tag:MIDITag?): Value {
            var strVal = valueStr
            strVal = strVal.trim()
            if (strVal.isEmpty())
                return NoValue()
            when {
                strVal == "*" -> return WildcardValue()
                strVal.startsWith("?") -> {
                    strVal = strVal.substring(1)
                    try {
                        val referencedArgIndex = Integer.parseInt(strVal)
                        if (referencedArgIndex < 0)
                            throw MalformedTagException(tag, BeatPrompterApplication.getResourceString(R.string.not_a_valid_argument_index))
                        else
                            return ArgumentValue(referencedArgIndex)
                    } catch (nfe: NumberFormatException) {
                        throw MalformedTagException(tag, BeatPrompterApplication.getResourceString(R.string.not_a_valid_argument_index))
                    }

                }
                strVal.startsWith("#") -> {
                    if (argIndex < argCount - 1)
                        throw MalformedTagException(tag, BeatPrompterApplication.getResourceString(R.string.channel_must_be_last_parameter))
                    try {
                        val channel = Utils.parseByte(strVal.substring(1))
                        if (channel < 1 || channel > 16)
                            throw MalformedTagException(tag, BeatPrompterApplication.getResourceString(R.string.invalid_channel_value))
                        return ChannelValue(channel)
                    } catch (nfe: NumberFormatException) {
                        throw MalformedTagException(tag, BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value))
                    }

                }
                strVal.contains("_") -> {
                    if (strVal.indexOf("_") != strVal.lastIndexOf("_"))
                        throw MalformedTagException(tag, BeatPrompterApplication.getResourceString(R.string.multiple_underscores_in_midi_value))
                    strVal = strVal.replace('_', '0')
                    try {
                        if (Utils.looksLikeHex(strVal)) {
                            val channelValue = Utils.parseHexByte(strVal)
                            if (channelValue and 0x0F != 0.toByte())
                                throw MalformedTagException(tag, BeatPrompterApplication.getResourceString(R.string.merge_with_channel_non_zero_lower_nibble))
                            else
                                return ChanneledCommandValue(channelValue)
                        } else
                            throw MalformedTagException(tag, BeatPrompterApplication.getResourceString(R.string.underscore_in_decimal_value))
                    } catch (nfe: NumberFormatException) {
                        throw MalformedTagException(tag, BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value))
                    }

                }
                Utils.looksLikeHex(strVal) -> try {
                    return CommandValue(Utils.parseHexByte(strVal))
                } catch (nfe: NumberFormatException) {
                    throw MalformedTagException(tag, BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value))
                }
                else -> try {
                    return CommandValue(Utils.parseByte(strVal))
                } catch (nfe: NumberFormatException) {
                    throw MalformedTagException(tag, BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value))
                }
            }
        }
    }
}