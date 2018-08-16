package com.stevenfrew.beatprompter.cache.parse

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.midi.SongTrigger
import java.util.ArrayList

class FileLine(line:String,private val mLineNumber:Int,errors:MutableList<FileParseError> = mutableListOf()) {
    private val mLine:String
    val mTaglessLine:String
    val mBars:Int
    val mScrollbeatDiff:Int
    val mTags:List<Tag>
    val isComment :Boolean
        get()=mLine.startsWith("#")
    val chordTags :List<Tag>
        get()= mTags.filter{it.isChord}
    val nonChordTags :List<Tag>
        get()= mTags.filterNot{it.isChord}
    val isEmpty:Boolean
        get()=mLine.isEmpty()

    init
    {
        if (line.length > MAX_LINE_LENGTH) {
            mLine = line.substring(0, MAX_LINE_LENGTH).trim()
            errors.add(FileParseError(null, BeatPrompterApplication.getResourceString(R.string.lineTooLong, mLineNumber, MAX_LINE_LENGTH)))
        }
        else
            mLine=line.trim()

        val tags = mutableListOf<Tag>()
        var strippedLine:String
        if(!isComment) {
            var workLine = mLine
            val lineOut = StringBuilder()
            var directiveStart = workLine.indexOf("{")
            var chordStart = workLine.indexOf("[")
            while (directiveStart != -1 || chordStart != -1) {
                val start: Int = if (directiveStart != -1)
                    if (chordStart != -1 && chordStart < directiveStart)
                        chordStart
                    else
                        directiveStart
                else
                    chordStart
                val tagCloser = if (start == directiveStart) "}" else "]"
                var end = workLine.indexOf(tagCloser, start + 1)
                if (end != -1) {
                    val contents = workLine.substring(start + 1, end).trim()
                    lineOut.append(workLine.substring(0, start))
                    workLine = workLine.substring(end + tagCloser.length)
                    end = 0
                    if (contents.trim().isNotEmpty())
                        try {
                            tags.add(Tag.parse(start == chordStart, contents, mLineNumber, lineOut.length))
                        }
                        catch(mte: MalformedTagException)
                        {
                            errors.add(FileParseError(mLineNumber,mte.message))
                        }
                } else
                    end = start + 1
                directiveStart = workLine.indexOf("{", end)
                chordStart = workLine.indexOf("[", end)
            }
            lineOut.append(workLine)
            strippedLine=lineOut.toString()
        }
        else
            strippedLine=mLine

        // Bars can be defined by commas ....
        var bars = 0
        while (strippedLine.startsWith(",")) {
            strippedLine = strippedLine.substring(1)
            tags.forEach{it.retreatPositionFrom(0)}
            bars++
        }

        // ... or by a tag (which overrides commas)
        for (tag in tags)
            if (!tag.isChord)
                if (tag.mName == "b" || tag.mName == "bars")
                    bars = tag.getIntegerValue(1, 128, 1, errors)

        mBars = Math.max(1, bars)

        var scrollbeatDiff=0
        while (strippedLine.endsWith(">") || strippedLine.endsWith("<")) {
            if (strippedLine.endsWith(">"))
                scrollbeatDiff++
            else if (strippedLine.endsWith("<"))
                scrollbeatDiff--
            strippedLine = strippedLine.substring(0, strippedLine.length - 1)
            tags.forEach{it.retreatPositionFrom(strippedLine.length)}
        }
        mScrollbeatDiff=scrollbeatDiff

        // TODO: dynamic BPB changing

        // Replace stupid unicode BOM character
        mTaglessLine = strippedLine.replace("\uFEFF", "")
        mTags=tags
    }

    fun getTitle(): String? {
        return getTokenValue("title", "t")
    }

    fun getKey(): String? {
        return getTokenValue("key")
    }

    private fun getFirstChordTag(): Tag? {
        return mTags.firstOrNull{it.isValidChord}
    }

    fun getFirstChord(): String? {
        return getFirstChordTag()?.mName
    }

    fun getArtist(): String? {
        return getTokenValue("artist", "a", "subtitle", "st")
    }

    fun getBPM(): String? {
        return getTokenValue("bpm", "beatsperminute", "metronome")
    }

    fun getTags(): List<String> {
        return getTokenValues("tag")
    }

    private fun getMIDITrigger(songSelectTrigger: Boolean): SongTrigger? {
        val `val` = getTokenValue(if (songSelectTrigger) "midi_song_select_trigger" else "midi_program_change_trigger")
        if (`val` != null)
            try {
                return SongTrigger.parse(`val`, songSelectTrigger, mLineNumber, ArrayList())
            } catch (e: Exception) {
                Log.e(BeatPrompterApplication.TAG, "Failed to parse MIDI song trigger from song file.", e)
            }
        return null
    }

    fun getMIDISongSelectTrigger(): SongTrigger? {
        return getMIDITrigger(true)
    }

    fun getMIDIProgramChangeTrigger(): SongTrigger? {
        return getMIDITrigger(false)
    }

    fun getAudioFiles(): List<String> {
        val audio = ArrayList<String>()
        audio.addAll(getTokenValues("audio"))
        audio.addAll(getTokenValues("track"))
        audio.addAll(getTokenValues("musicpath"))
        val realAudio = ArrayList<String>()
        for (str in audio) {
            var audioString=str
            val index = audioString.indexOf(":")
            if (index != -1 && index < audioString.length - 1)
                audioString = audioString.substring(0, index)
            realAudio.add(audioString)
        }
        return realAudio
    }

    fun getImageFiles(): ArrayList<String> {
        val image = ArrayList(getTokenValues("image"))
        val realimage = ArrayList<String>()
        for (str in image) {
            var imageString=str
            val index = imageString.indexOf(":")
            if (index != -1 && index < imageString.length - 1)
                imageString = imageString.substring(0, index)
            realimage.add(imageString)
        }
        return realimage
    }

    private fun getTokenValues(vararg tokens: String): List<String> {
        return mTags.filter{tokens.contains(it.mName)}.map{it.mValue.trim()}
    }

    internal fun getTokenValue(vararg tokens: String): String? {
        return getTokenValues(*tokens).lastOrNull()
    }

    fun containsToken(tokenToFind: String): Boolean {
        return getTokenValues(tokenToFind).any()
    }

    companion object {
        private const val MAX_LINE_LENGTH = 256
    }
}