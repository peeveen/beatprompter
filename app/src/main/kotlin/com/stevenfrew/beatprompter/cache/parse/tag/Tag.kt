package com.stevenfrew.beatprompter.cache.parse.tag

import android.graphics.Color
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Utils
import com.stevenfrew.beatprompter.cache.parse.SongParsingState
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import java.io.File
import java.util.*

abstract class Tag protected constructor(val mName: String, internal val mLineNumber: Int, val mPosition: Int) {

    val isColorTag:Boolean
        get()= colorTags.contains(mName)

    val isOneShotTag:Boolean
        get()= oneShotTags.contains(mName)

    @Throws(MalformedTagException::class)
    fun parseIntegerValue(value:String,min: Int, max: Int): Int {
        val intVal: Int
        try {
            intVal = value.toInt()
            if (intVal < min)
                throw MalformedTagException(this,BeatPrompterApplication.getResourceString(R.string.intValueTooLow, min, intVal))
            else if (intVal > max)
                throw MalformedTagException(this,BeatPrompterApplication.getResourceString(R.string.intValueTooHigh, max, intVal))
        } catch (nfe: NumberFormatException) {
            throw MalformedTagException(this,BeatPrompterApplication.getResourceString(R.string.intValueUnreadable, value))
        }
        return intVal
    }

    fun parseDurationValue(value:String,min: Long, max: Long, trackLengthAllowed: Boolean): Long {
        val durVal: Long
        try {
            durVal = Utils.parseDuration(value, trackLengthAllowed)
            if (durVal < min && durVal != Utils.TRACK_AUDIO_LENGTH_VALUE)
                throw MalformedTagException(this,BeatPrompterApplication.getResourceString(R.string.intValueTooLow, min, durVal))
            else if (durVal > max)
                throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.intValueTooHigh, max, durVal))
        } catch (nfe: NumberFormatException) {
            throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.durationValueUnreadable, value))
        }

        return durVal
    }

    fun parseDoubleValue(value:String,min: Double, max: Double): Double {
        val doubleVal: Double
        try {
            doubleVal = value.toDouble()
            if (doubleVal < min)
                throw MalformedTagException(this,BeatPrompterApplication.getResourceString(R.string.doubleValueTooLow, min, doubleVal))
            else if (doubleVal > max)
                throw MalformedTagException(this,BeatPrompterApplication.getResourceString(R.string.doubleValueTooHigh, max, doubleVal))
        } catch (nfe: NumberFormatException) {
            throw MalformedTagException(this,BeatPrompterApplication.getResourceString(R.string.doubleValueUnreadable, value))
        }
        return doubleVal
    }

    fun parseColourValue(value:String): Int {
        return try {
            Color.parseColor(value)
        } catch (iae: IllegalArgumentException) {
            try {
                Color.parseColor("#$value")
            } catch (iae2: IllegalArgumentException) {
                throw MalformedTagException(this,BeatPrompterApplication.getResourceString(R.string.colorValueUnreadable, value))
            }
        }
    }

    companion object {
        val colorTags: HashSet<String> = hashSetOf("backgroundcolour", "bgcolour", "backgroundcolor", "bgcolor", "pulsecolour", "beatcolour", "pulsecolor", "beatcolor", "lyriccolour", "lyricscolour", "lyriccolor", "lyricscolor", "chordcolour", "chordcolor", "commentcolour", "commentcolor", "beatcountercolour", "beatcountercolor")
        val oneShotTags: HashSet<String> = hashSetOf("title", "t", "artist", "a", "subtitle", "st", "count", "trackoffset", "time", "midi_song_select_trigger", "midi_program_change_trigger")

        @Throws(MalformedTagException::class)
        fun parse(tagContents:String,lineNumber:Int,position:Int,sourceFile: File,parsingState: SongParsingState):Tag{
            var txt=tagContents
            val chord=txt.startsWith('[')
            txt=if(chord)txt.trim('[',']')else txt.trim('{','}')
            txt=txt.trim()
            var colonIndex = txt.indexOf(":")
            val spaceIndex = txt.indexOf(" ")
            if (colonIndex == -1)
                colonIndex = spaceIndex
            val name:String
            val value:String
            if (colonIndex == -1) {
                name = if (chord) txt else txt.toLowerCase()
                value = ""
            } else {
                name = if (chord) txt else txt.substring(0, colonIndex).toLowerCase()
                value = txt.substring(colonIndex + 1).trim()
            }
            if(chord)
                return ChordTag(name, lineNumber, position)
            when(name)
            {
                "b","bars"->return BarsTag(name, lineNumber, position, value)
                "bpm", "metronome", "beatsperminute"->return BeatsPerMinuteTag(name, lineNumber, position, value)
                "bpb", "beatsperbar"->return BeatsPerBarTag(name, lineNumber, position, value)
                "bpl", "barsperline"->return BarsPerLineTag(name, lineNumber, position, value)
                "scrollbeat", "sb"->return ScrollBeatTag(name, lineNumber, position, value)
                "beatstart"->return BeatStartTag(name, lineNumber, position)
                "beatstop"->return BeatStopTag(name, lineNumber, position)

                "time" -> return TimeTag(name, lineNumber, position, value)
                "pause"->return PauseTag(name, lineNumber, position, value)
                "image"->return ImageTag(name, lineNumber, position, value, sourceFile, parsingState.mTempImageFileCollection)
                "track", "audio", "musicpath"->return TrackTag(name, lineNumber, position, value, sourceFile, parsingState.mTempAudioFileCollection)
                "send_midi_clock"->return SendMIDIClockTag(name, lineNumber, position)
                "comment", "c", "comment_box", "cb", "comment_italic", "ci"->return CommentTag(name, lineNumber, position, value)
                "midi_song_select_trigger"->return MIDISongSelectTriggerTag(name, lineNumber, position, value)
                "midi_program_change_trigger"->return MIDIProgramChangeTriggerTag(name, lineNumber, position, value)

                "backgroundcolour", "backgroundcolor", "bgcolour", "bgcolor"->return BackgroundColorTag(name, lineNumber, position, value)
                "pulsecolour", "pulsecolor", "beatcolour", "beatcolor"->return PulseColorTag(name, lineNumber, position, value)
                "lyriccolour", "lyriccolor", "lyricscolour", "lyricscolor"->return LyricsColorTag(name, lineNumber, position, value)
                "chordcolour", "chordcolor" ->return ChordsColorTag(name, lineNumber, position, value)
                "beatcountercolour", "beatcountercolor"->return BeatCounterColorTag(name, lineNumber, position, value)

                "soh"->return StartOfHighlightTag(name, lineNumber, position, value, parsingState.mCurrentHighlightColor)
                "eoh"->return EndOfHighlightTag(name, lineNumber, position)

                "title", "t" ->return TitleTag(name, lineNumber, position, value)
                "artist", "a", "subtitle", "st"->return ArtistTag(name, lineNumber, position, value)
                "key"->return KeyTag(name, lineNumber, position, value)
                "tag"->return TagTag(name, lineNumber, position, value)

                // Unused ChordPro tags
                "start_of_chorus", "end_of_chorus", "start_of_tab", "end_of_tab", "soc", "eoc", "sot", "eot", "define", "textfont", "tf", "textsize", "ts", "chordfont", "cf", "chordsize", "cs", "no_grid", "ng", "grid", "g", "titles", "new_page", "np", "new_physical_page", "npp", "columns", "col", "column_break", "colb", "pagetype", "capo", "zoom-android", "zoom", "tempo", "tempo-android", "instrument", "tuning" -> {}

                else->return MIDIEventTag(name, lineNumber, position, value, parsingState.mSongTime, parsingState.mDefaultMIDIChannel)
            }
            throw UnknownTagException(name)
        }
    }
}
