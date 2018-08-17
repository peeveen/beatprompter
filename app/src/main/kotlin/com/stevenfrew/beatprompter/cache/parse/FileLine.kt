package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.*
import com.stevenfrew.beatprompter.cache.parse.tag.song.*

abstract class FileLine<TParsingState:FileParsingState>(line:String, private val mLineNumber:Int, parsingState:TParsingState) {

    data class TagText constructor(val mText:String,val mPosition:Int)

    private val mLine:String
    var mTaglessLine:String

    val mTags:List<Tag>
    val isComment :Boolean
        get()=mLine.startsWith("#")
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

        val textTags = mutableListOf<TagText>()
        val strippedLine:String
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

        // Replace stupid unicode BOM character
        mTaglessLine = strippedLine.replace("\uFEFF", "")
        mTags=parseTags(textTags,parsingState)
    }

    private fun parseTags(textTags:List<TagText>,parsingState:TParsingState):List<Tag>
    {
        return textTags.mapNotNull { tt->
            try {
                parseTag(tt.mText,mLineNumber,tt.mPosition,parsingState)
            }
            catch(mte:MalformedTagException) {
                parsingState.mErrors.add(FileParseError(mLineNumber,mte.message))
                null
            }
        }
    }

    abstract fun parseTag(text:String,lineNumber:Int,position:Int,parsingState:TParsingState):Tag

    fun getTags(): List<String> {
        return mTags.filterIsInstance<TagTag>().map{it.mTag}
    }

    companion object {
        private const val MAX_LINE_LENGTH = 256
    }
}