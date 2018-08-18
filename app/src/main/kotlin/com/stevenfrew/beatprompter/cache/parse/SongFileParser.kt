package com.stevenfrew.beatprompter.cache.parse

import android.graphics.Color
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.MetronomeContext
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.ImageFile
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.midi.Message
import com.stevenfrew.beatprompter.midi.TriggerOutputContext

abstract class SongFileParser<TResultType> constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, currentAudioFiles:List<AudioFile>, currentImageFiles:List<ImageFile>):TextFileParser<TResultType>(cachedCloudFileDescriptor) {

    val mCurrentAudioFiles=currentAudioFiles
    val mCurrentImageFiles=currentImageFiles
    val mIgnoreColorInfo:Boolean
    val mCountInPref:Int
    val mMetronomeContext:MetronomeContext
    val mCustomCommentsUser:String
    val mShowChords:Boolean
    val mTriggerContext:TriggerOutputContext

    var mBackgroundColor:Int
    var mPulseColor:Int
    var mBeatCounterColor:Int
    var mScrollMarkerColor:Int
    var mLyricColor:Int
    var mChordColor:Int
    var mAnnotationColor:Int
    var mSendMidiClock:Boolean=false
    var mCurrentHighlightColor:Int
    var mSongTime:Long=0
    var mDefaultMIDIOutputChannel:Byte

    init
    {
        val sharedPrefs=BeatPrompterApplication.preferences
        mIgnoreColorInfo = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_key), BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_defaultValue).toBoolean())
        mSendMidiClock = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_sendMidi_key), false)
        mCountInPref = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_default)))
        mMetronomeContext = MetronomeContext.getMetronomeContextPreference(sharedPrefs)
        mBackgroundColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_default)))
        mPulseColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_pulseColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_pulseColor_default)))
        mCurrentHighlightColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_default)))
        mBeatCounterColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_default)))
        mScrollMarkerColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_scrollMarkerColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_scrollMarkerColor_default)))
        mLyricColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_lyricColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_lyricColor_default)))
        mChordColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_chordColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_chordColor_default)))
        mAnnotationColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_annotationColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_annotationColor_default)))
        mCustomCommentsUser = sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_customComments_key), BeatPrompterApplication.getResourceString(R.string.pref_customComments_defaultValue))?:""
        mShowChords = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showChords_key), BeatPrompterApplication.getResourceString(R.string.pref_showChords_defaultValue).toBoolean())
        mTriggerContext = TriggerOutputContext.valueOf(sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_key), BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_defaultValue))!!)
        val defaultMIDIOutputChannelPrefValue = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_default)))
        mDefaultMIDIOutputChannel = Message.getChannelFromBitmask(defaultMIDIOutputChannelPrefValue)
    }

    override fun parseTag(text:String,lineNumber:Int,position:Int):Tag
    {
        var txt=text
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
            "image"->return ImageTag(name, lineNumber, position, value, mCachedCloudFileDescriptor.mFile, mCurrentImageFiles)
            "track", "audio", "musicpath"->return TrackTag(name, lineNumber, position, value, mCachedCloudFileDescriptor.mFile, mCurrentAudioFiles)
            "send_midi_clock"->return SendMIDIClockTag(name, lineNumber, position)
            "comment", "c", "comment_box", "cb", "comment_italic", "ci"->return CommentTag(name, lineNumber, position, value)
            "count","countin"->return CountTag(name,lineNumber,position,value)
            "midi_song_select_trigger"->return MIDISongSelectTriggerTag(name, lineNumber, position, value)
            "midi_program_change_trigger"->return MIDIProgramChangeTriggerTag(name, lineNumber, position, value)

            "backgroundcolour", "backgroundcolor", "bgcolour", "bgcolor"->return BackgroundColorTag(name, lineNumber, position, value)
            "pulsecolour", "pulsecolor", "beatcolour", "beatcolor"->return PulseColorTag(name, lineNumber, position, value)
            "lyriccolour", "lyriccolor", "lyricscolour", "lyricscolor"->return LyricsColorTag(name, lineNumber, position, value)
            "chordcolour", "chordcolor" ->return ChordsColorTag(name, lineNumber, position, value)
            "beatcountercolour", "beatcountercolor"->return BeatCounterColorTag(name, lineNumber, position, value)

            "soh"->return StartOfHighlightTag(name, lineNumber, position, value, mCurrentHighlightColor)
            "eoh"->return EndOfHighlightTag(name, lineNumber, position)

            "title", "t" ->return TitleTag(name, lineNumber, position, value)
            "artist", "a", "subtitle", "st"->return ArtistTag(name, lineNumber, position, value)
            "key"->return KeyTag(name, lineNumber, position, value)
            "tag"->return TagTag(name, lineNumber, position, value)

            // Unused ChordPro tags
            "start_of_chorus", "end_of_chorus", "start_of_tab", "end_of_tab", "soc", "eoc", "sot", "eot", "define", "textfont", "tf", "textsize", "ts", "chordfont", "cf", "chordsize", "cs", "no_grid", "ng", "grid", "g", "titles", "new_page", "np", "new_physical_page", "npp", "columns", "col", "column_break", "colb", "pagetype", "capo", "zoom-android", "zoom", "tempo", "tempo-android", "instrument", "tuning" -> return ChordProTag(name,lineNumber,position)

            else->return MIDIEventTag(name, lineNumber, position, value, mSongTime, mDefaultMIDIOutputChannel)
        }
    }
}