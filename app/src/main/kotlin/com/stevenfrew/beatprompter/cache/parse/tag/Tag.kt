package com.stevenfrew.beatprompter.cache.parse.tag

import android.graphics.Color
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Utils
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.event.MIDIEvent
import com.stevenfrew.beatprompter.midi.*
import java.util.*

class Tag private constructor(private val mChordTag: Boolean, str: String, internal var mLineNumber: Int, var mPosition: Int) {
    var mName: String
    val mValue: String

    val isChord:Boolean
        get()=mChordTag

    val isValidChord:Boolean
        get()=mChordTag && Utils.isChord(mName)

    val isColorTag:Boolean
        get()= colorTags.contains(mName)

    val isOneShotTag:Boolean
        get()= oneShotTags.contains(mName)

    init {
        var colonIndex = str.indexOf(":")
        val spaceIndex = str.indexOf(" ")
        if (colonIndex == -1)
            colonIndex = spaceIndex
        if (colonIndex == -1) {
            mName = if (mChordTag) str.trim() else str.toLowerCase().trim()
            mValue = ""
        } else {
            mName = if (mChordTag) str.trim() else str.substring(0, colonIndex).toLowerCase().trim()
            mValue = str.substring(colonIndex + 1).trim()
        }
    }

    fun parsePotentialCommentTag(): String
    {
        if(mName.contains('@')) {
            val bit = mName.substringBefore('@')
            when (bit) {
                "comment", "c", "comment_box", "cb", "comment_italic", "ci" -> {
                    mName = "comment"
                    return mName.substringAfter('@')
                }
            }
        }
        return ""
    }

    fun getMIDIEvent(time: Long, aliases: List<Alias>, defaultChannel: Byte, parseErrors: MutableList<FileParseError>): MIDIEvent? {
        var tagValue = mValue.trim()
        var eventOffset: EventOffset = EventOffset.NoOffset
        if (tagValue.isEmpty()) {
            // A MIDI tag of {blah;+33} ends up with "blah;+33" as the tag name. Fix it here.
            if (mName.contains(";")) {
                val bits = mName.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (bits.size > 2)
                    parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.multiple_semi_colons_in_midi_tag)))
                if (bits.size > 1) {
                    eventOffset = EventOffset(bits[1].trim(), this, parseErrors)
                    mName = bits[0].trim()
                }
            }
        } else {
            val firstSplitBits = tagValue.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (firstSplitBits.size > 1) {
                if (firstSplitBits.size > 2)
                    parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.multiple_semi_colons_in_midi_tag)))
                tagValue = firstSplitBits[0].trim()
                eventOffset = EventOffset(firstSplitBits[1].trim(), this, parseErrors)
            }
        }
        val bits = if (tagValue.isEmpty()) arrayOf() else tagValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var paramBytes = bits.map { bit -> Value.parseValue(bit.trim()) }
        var lastParamIsChannel = false
        var channel = defaultChannel
        for (f in paramBytes.indices)
            if (paramBytes[f] is ChannelValue)
                if (f == paramBytes.size - 1)
                    lastParamIsChannel = true
                else
                    parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.channel_must_be_last_parameter)))
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
                if (alias.mName.equals(mName, ignoreCase = true)) {
                    return MIDIEvent(time, alias.resolve(aliases, resolvedBytes, channel), eventOffset)
                }
            if (mName == "midi_send")
                return MIDIEvent(time, OutgoingMessage(resolvedBytes), eventOffset)
            parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.unknown_midi_directive, mName)))
        } catch (re: ResolutionException) {
            parseErrors.add(FileParseError(this.mLineNumber, re.message))
        }

        return null

    }

    fun getIntegerValue(min: Int, max: Int, defolt: Int, parseErrors: MutableList<FileParseError>): Int {
        var intVal: Int
        try {
            intVal = mValue.toInt()
            if (intVal < min) {
                parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.intValueTooLow, min, intVal)))
                intVal = min
            } else if (intVal > max) {
                parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.intValueTooHigh, max, intVal)))
                intVal = max
            }
        } catch (nfe: NumberFormatException) {
            parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.intValueUnreadable, mValue, defolt)))
            intVal = defolt
        }
        return intVal
    }

    fun getDurationValue(min: Int, max: Int, defolt: Int, trackLengthAllowed: Boolean, parseErrors: MutableList<FileParseError>): Int {
        var intVal: Int
        try {
            intVal = Utils.parseDuration(mValue, trackLengthAllowed)
            if (intVal < min && intVal != Utils.TRACK_AUDIO_LENGTH_VALUE) {
                parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.intValueTooLow, min, intVal)))
                intVal = min
            } else if (intVal > max) {
                parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.intValueTooHigh, max, intVal)))
                intVal = max
            }
        } catch (nfe: NumberFormatException) {
            parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.durationValueUnreadable, mValue, defolt)))
            intVal = defolt
        }

        return intVal
    }

    fun getDoubleValue(min: Double, max: Double, defolt: Double, parseErrors: MutableList<FileParseError>): Double {
        var intVal: Double
        try {
            intVal = java.lang.Double.parseDouble(mValue)
            if (intVal < min) {
                parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.doubleValueTooLow, min, intVal)))
                intVal = min
            } else if (intVal > max) {
                parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.doubleValueTooHigh, max, intVal)))
                intVal = max
            }
        } catch (nfe: NumberFormatException) {
            parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.doubleValueUnreadable, mValue, defolt)))
            intVal = defolt
        }
        return intVal
    }

    fun getColourValue(defolt: Int, parseErrors: MutableList<FileParseError>): Int {
        try {
            return Color.parseColor(mValue)
        } catch (iae: IllegalArgumentException) {
            try {
                return Color.parseColor("#$mValue")
            } catch (iae2: IllegalArgumentException) {
                var defaultString = "000000" + Integer.toHexString(defolt)
                defaultString = defaultString.substring(defaultString.length - 6)
                parseErrors.add(FileParseError(this, BeatPrompterApplication.getResourceString(R.string.colorValueUnreadable, mValue, defaultString)))
            }

        }
        return defolt
    }

    internal fun retreatPositionFrom(position:Int)
    {
        if(mPosition>position)
            --mPosition
    }

    companion object {
        val colorTags: HashSet<String> = hashSetOf("backgroundcolour", "bgcolour", "backgroundcolor", "bgcolor", "pulsecolour", "beatcolour", "pulsecolor", "beatcolor", "lyriccolour", "lyricscolour", "lyriccolor", "lyricscolor", "chordcolour", "chordcolor", "commentcolour", "commentcolor", "beatcountercolour", "beatcountercolor")
        val oneShotTags: HashSet<String> = hashSetOf("title", "t", "artist", "a", "subtitle", "st", "count", "trackoffset", "time", "midi_song_select_trigger", "midi_program_change_trigger")

        fun verifySongTriggerFromTag(tag: Tag, parseErrors: MutableList<FileParseError>) {
            try {
                SongTrigger.parse(tag.mValue, tag.mName == "midi_song_select_trigger", tag.mLineNumber, parseErrors)
            } catch (e: Exception) {
                parseErrors.add(FileParseError(tag, e.message))
            }
        }

        fun parse(chordTag:Boolean,tagContents:String,lineNumber:Int,position:Int):Tag{
            return Tag(chordTag,tagContents,lineNumber,position)
        }
    }
}
