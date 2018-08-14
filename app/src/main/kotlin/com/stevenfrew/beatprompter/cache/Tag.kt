package com.stevenfrew.beatprompter.cache

import android.graphics.Color
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Utils
import com.stevenfrew.beatprompter.event.MIDIEvent
import com.stevenfrew.beatprompter.midi.*
import java.util.*

class Tag private constructor(var mChordTag: Boolean, str: String, internal var mLineNumber: Int, var mPosition: Int) {
    var mName: String
    var mValue: String

    init {
        var colonIndex = str.indexOf(":")
        val spaceIndex = str.indexOf(" ")
        if (colonIndex == -1)
            colonIndex = spaceIndex
        if (colonIndex == -1) {
            mName = if (mChordTag) str else str.toLowerCase()
            mValue = ""
        } else {
            mName = if (mChordTag) str else str.substring(0, colonIndex).toLowerCase()
            mValue = str.substring(colonIndex + 1)
        }
    }

    companion object {
        val colorTags: HashSet<String> = hashSetOf("backgroundcolour", "bgcolour", "backgroundcolor", "bgcolor", "pulsecolour", "beatcolour", "pulsecolor", "beatcolor", "lyriccolour", "lyricscolour", "lyriccolor", "lyricscolor", "chordcolour", "chordcolor", "commentcolour", "commentcolor", "beatcountercolour", "beatcountercolor")
        val oneShotTags: HashSet<String> = hashSetOf("title", "t", "artist", "a", "subtitle", "st", "count", "trackoffset", "time", "midi_song_select_trigger", "midi_program_change_trigger")

        fun getMIDIEventFromTag(time: Long, tag: Tag, aliases: ArrayList<Alias>, defaultChannel: Byte, parseErrors: ArrayList<FileParseError>): MIDIEvent? {
            var tagValue = tag.mValue.trim()
            var eventOffset: EventOffset? = null
            if (tagValue.isEmpty()) {
                // A MIDI tag of {blah;+33} ends up with "blah;+33" as the tag name. Fix it here.
                if (tag.mName.contains(";")) {
                    val bits = tag.mName.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (bits.size > 2)
                        parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.multiple_semi_colons_in_midi_tag)))
                    if (bits.size > 1) {
                        eventOffset = EventOffset(bits[1].trim(), tag, parseErrors)
                        tag.mName = bits[0].trim()
                    }
                }
            } else {
                val firstSplitBits = tagValue.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (firstSplitBits.size > 1) {
                    if (firstSplitBits.size > 2)
                        parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.multiple_semi_colons_in_midi_tag)))
                    tagValue = firstSplitBits[0].trim()
                    eventOffset = EventOffset(firstSplitBits[1].trim(), tag, parseErrors)
                }
            }
            val bits = if (tagValue.isEmpty()) arrayOf() else tagValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var paramBytes = bits.map{ bit -> Value.parseValue(bit.trim())}
            var lastParamIsChannel = false
            var channel = defaultChannel
            for (f in paramBytes.indices)
                if (paramBytes[f] is ChannelValue)
                    if (f == paramBytes.size - 1)
                        lastParamIsChannel = true
                    else
                        parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.channel_must_be_last_parameter)))
            try {
                if (lastParamIsChannel) {
                    val lastParam = paramBytes[paramBytes.size - 1] as ChannelValue
                    channel = lastParam.resolve()
                    val paramBytesWithoutChannel = paramBytes.subList(0,paramBytes.size - 1)
                    paramBytes = paramBytesWithoutChannel
                }
                val resolvedBytes = ByteArray(paramBytes.size)
                for (f in paramBytes.indices)
                    resolvedBytes[f] = paramBytes[f].resolve()
                for (alias in aliases)
                    if (alias.mName.equals(tag.mName, ignoreCase = true)) {
                        return MIDIEvent(time, alias.resolve(aliases, resolvedBytes, channel), eventOffset!!)
                    }
                if (tag.mName == "midi_send")
                    return MIDIEvent(time, OutgoingMessage(resolvedBytes), eventOffset!!)
                parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.unknown_midi_directive, tag.mName)))
            } catch (re: ResolutionException) {
                parseErrors.add(FileParseError(tag.mLineNumber, re.message))
            }

            return null

        }

        fun getIntegerValueFromTag(tag: Tag, min: Int, max: Int, defolt: Int, parseErrors: ArrayList<FileParseError>): Int {
            var `val`: Int
            try {
                `val` = Integer.parseInt(tag.mValue)
                if (`val` < min) {
                    parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.intValueTooLow, min, `val`)))
                    `val` = min
                } else if (`val` > max) {
                    parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.intValueTooHigh, max, `val`)))
                    `val` = max
                }
            } catch (nfe: NumberFormatException) {
                parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.intValueUnreadable, tag.mValue, defolt)))
                `val` = defolt
            }

            return `val`
        }

        fun getDurationValueFromTag(tag: Tag, min: Int, max: Int, defolt: Int, trackLengthAllowed: Boolean, parseErrors: ArrayList<FileParseError>): Int {
            var `val`: Int
            try {
                `val` = Utils.parseDuration(tag.mValue, trackLengthAllowed)
                if (`val` < min && `val` != Utils.TRACK_AUDIO_LENGTH_VALUE) {
                    parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.intValueTooLow, min, `val`)))
                    `val` = min
                } else if (`val` > max) {
                    parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.intValueTooHigh, max, `val`)))
                    `val` = max
                }
            } catch (nfe: NumberFormatException) {
                parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.durationValueUnreadable, tag.mValue, defolt)))
                `val` = defolt
            }

            return `val`
        }

        fun getDoubleValueFromTag(tag: Tag, min: Double, max: Double, defolt: Double, parseErrors: ArrayList<FileParseError>): Double {
            var `val`: Double
            try {
                `val` = java.lang.Double.parseDouble(tag.mValue)
                if (`val` < min) {
                    parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.doubleValueTooLow, min, `val`)))
                    `val` = min
                } else if (`val` > max) {
                    parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.doubleValueTooHigh, max, `val`)))
                    `val` = max
                }
            } catch (nfe: NumberFormatException) {
                parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.doubleValueUnreadable, tag.mValue, defolt)))
                `val` = defolt
            }

            return `val`
        }

        fun getColourValueFromTag(tag: Tag, defolt: Int, parseErrors: ArrayList<FileParseError>): Int {
            try {
                return Color.parseColor(tag.mValue)
            } catch (iae: IllegalArgumentException) {
                try {
                    return Color.parseColor("#" + tag.mValue)
                } catch (iae2: IllegalArgumentException) {
                    var defaultString = "000000" + Integer.toHexString(defolt)
                    defaultString = defaultString.substring(defaultString.length - 6)
                    parseErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.colorValueUnreadable, tag.mValue, defaultString)))
                }

            }

            return defolt
        }

        fun verifySongTriggerFromTag(tag: Tag, parseErrors: ArrayList<FileParseError>) {
            try {
                SongTrigger.parse(tag.mValue, tag.mName == "midi_song_select_trigger", tag.mLineNumber, parseErrors)
            } catch (e: Exception) {
                parseErrors.add(FileParseError(tag, e.message))
            }

        }

        fun extractTags(lineStr: String, lineNumber: Int, tagsOut: ArrayList<Tag>): String {
            var line = lineStr
            tagsOut.clear()
            val lineOut = StringBuilder()
            var directiveStart = line.indexOf("{")
            var chordStart = line.indexOf("[")
            while (directiveStart != -1 || chordStart != -1) {
                val start: Int = if (directiveStart != -1)
                    if (chordStart != -1 && chordStart < directiveStart)
                        chordStart
                    else
                        directiveStart
                else
                    chordStart
                val tagCloser = if (start == directiveStart) "}" else "]"
                var end = line.indexOf(tagCloser, start + 1)
                if (end != -1) {
                    val contents = line.substring(start + 1, end).trim()
                    lineOut.append(line.substring(0, start))
                    line = line.substring(end + tagCloser.length)
                    end = 0
                    if (contents.trim().isNotEmpty())
                        tagsOut.add(Tag(start == chordStart, contents, lineNumber, lineOut.length))
                } else
                    end = start + 1
                directiveStart = line.indexOf("{", end)
                chordStart = line.indexOf("[", end)
            }
            lineOut.append(line)
            return lineOut.toString()
        }
    }
}
