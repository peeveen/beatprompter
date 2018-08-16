package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.ImageFile
import com.stevenfrew.beatprompter.cache.parse.tag.*
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.midi.SongTrigger
import java.io.File

open class FileLine(line:String, private val mLineNumber:Int, sourceFile: File, parsingState:SongParsingState) {

    data class TagText constructor(val mText:String,val mPosition:Int)

    private val mLine:String
    val mTaglessLine:String
    val mBPM:Double
    private val mBPB:Int
    val mBars:Int
    private val mScrollBeat:Int

    val mTags:List<Tag>
    val isComment :Boolean
        get()=mLine.startsWith("#")
    val chordTags :List<Tag>
        get()= mTags.filter{it is ChordTag }
    val nonChordTags :List<Tag>
        get()= mTags.filterNot{it is ChordTag }
    val isEmpty:Boolean
        get()=mLine.isEmpty()

    init
    {
        if (line.length > MAX_LINE_LENGTH) {
            mLine = line.substring(0, MAX_LINE_LENGTH).trim()
            parsingState.mErrors.add(FileParseError(null, BeatPrompterApplication.getResourceString(R.string.lineTooLong, mLineNumber, MAX_LINE_LENGTH)))
        }
        else
            mLine=line.trim()

        var textTags = mutableListOf<TagText>()
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
                val tagStarter = if (start == directiveStart) "{" else "["
                var end = workLine.indexOf(tagCloser, start + 1)
                if (end != -1) {
                    val contents = workLine.substring(start + 1, end).trim()
                    lineOut.append(workLine.substring(0, start))
                    workLine = workLine.substring(end + tagCloser.length)
                    end = 0
                    if (contents.trim().isNotEmpty())
                        try {
                            textTags.add(TagText(tagStarter+contents+tagCloser, lineOut.length))
                        }
                        catch(mte: MalformedTagException)
                        {
                            parsingState.mErrors.add(FileParseError(mLineNumber,mte.message))
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
        var commaBars=0
        while (strippedLine.startsWith(",")) {
            strippedLine = strippedLine.substring(1)
            textTags=textTags.map{if(it.mPosition>0) it else TagText(it.mText,it.mPosition-1)}.toMutableList()
            commaBars++
        }

        var scrollbeatDiff=0
        while (strippedLine.endsWith(">") || strippedLine.endsWith("<")) {
            if (strippedLine.endsWith(">"))
                scrollbeatDiff++
            else if (strippedLine.endsWith("<"))
                scrollbeatDiff--
            strippedLine = strippedLine.substring(0, strippedLine.length - 1)
            textTags=textTags.map{if(it.mPosition>strippedLine.length) it else TagText(it.mText,it.mPosition-1)}.toMutableList()
        }

        // TODO: dynamic BPB changing

        // Replace stupid unicode BOM character
        mTaglessLine = strippedLine.replace("\uFEFF", "")
        mTags= textTags.mapNotNull { tt->
            try {
                Tag.parse(tt.mText,mLineNumber,tt.mPosition,sourceFile,parsingState)
            }
            catch(mte:MalformedTagException) {
                parsingState.mErrors.add(FileParseError(mLineNumber,mte.message))
                null
            } catch(ute: UnknownTagException) {
                parsingState.mErrors.add(FileParseError(mLineNumber,ute.message))
                null
            }
        }

        // ... or by a tag (which overrides commas)
        val barsTag=mTags.filterIsInstance<BarsTag>().firstOrNull()
        val barsPerLineTag=mTags.filterIsInstance<BarsPerLineTag>().firstOrNull()
        val beatsPerBarTag=mTags.filterIsInstance<BeatsPerBarTag>().firstOrNull()
        val beatsPerMinuteTag=mTags.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
        val scrollBeatTag=mTags.filterIsInstance<ScrollBeatTag>().firstOrNull()

        val barsInThisLine=barsTag?.mBars?:barsPerLineTag?.mBPL?:commaBars?:parsingState.mBPL
        val beatsPerBarInThisLine=beatsPerBarTag?.mBPB?:parsingState.mBPB
        val beatsPerMinuteInThisLine=beatsPerMinuteTag?.mBPM?:parsingState.mBPM
        var scrollBeatInThisLine=scrollBeatTag?.mScrollBeat?:parsingState.mScrollBeat

        parsingState.mBPB=beatsPerBarInThisLine
        parsingState.mBPM=beatsPerMinuteInThisLine
        parsingState.mBPL=barsPerLineTag?.mBPL?:parsingState.mBPL
        parsingState.mScrollBeat=scrollBeatInThisLine

        if(scrollBeatInThisLine>beatsPerBarInThisLine)
            scrollBeatInThisLine=beatsPerBarInThisLine-1
        scrollBeatInThisLine+=scrollbeatDiff

        // TODO: SCROLLBEAT WONKIFIED? PARSE ERROR

        mBars=barsInThisLine
        mBPB=beatsPerBarInThisLine
        mBPM=beatsPerMinuteInThisLine
        mScrollBeat=scrollBeatInThisLine
    }

    fun getTitle(): String? {
        return mTags.filterIsInstance<TitleTag>().firstOrNull()?.mTitle
    }

    fun getKey(): String? {
        return mTags.filterIsInstance<KeyTag>().firstOrNull()?.mKey
    }

    private fun getFirstChordTag(): Tag? {
        return mTags.filterIsInstance<ChordTag>().firstOrNull {it.isValidChord()}
    }

    fun getFirstChord(): String? {
        return getFirstChordTag()?.mName
    }

    fun getArtist(): String? {
        return mTags.filterIsInstance<ArtistTag>().firstOrNull()?.mArtist
    }

    fun getBPM(): Double? {
        return mTags.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()?.mBPM
    }

    fun getTags(): List<String> {
        return mTags.filterIsInstance<TagTag>().map{it.mTag}
    }

    fun getMIDISongSelectTrigger(): SongTrigger? {
        return mTags.filterIsInstance<MIDISongSelectTriggerTag>().firstOrNull()?.mTrigger
    }

    fun getMIDIProgramChangeTrigger(): SongTrigger? {
        return mTags.filterIsInstance<MIDIProgramChangeTriggerTag>().firstOrNull()?.mTrigger
    }

    fun getAudioFiles(): List<AudioFile> {
        return mTags.filterIsInstance<TrackTag>().map{it.mAudioFile}
    }

    fun getImageFiles(): List<ImageFile> {
        return mTags.filterIsInstance<ImageTag>().map{it.mImageFile}
    }

    companion object {
        private const val MAX_LINE_LENGTH = 256
    }
}