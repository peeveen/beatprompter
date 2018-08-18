package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.*

class TextFileLine<TFileType>(line:String, val mLineNumber:Int, parser:TextFileParser<TFileType>) {

    data class TagText constructor(val mText:String,val mPosition:Int)

    private val mLine:String
    var mTaglessLine:String

    val mTags:List<Tag>
    val isEmpty:Boolean
        get()=mLine.isEmpty()

    init
    {
        if (line.length > MAX_LINE_LENGTH) {
            mLine = line.substring(0, MAX_LINE_LENGTH).trim()
            parser.addError(FileParseError(mLineNumber,BeatPrompterApplication.getResourceString(R.string.lineTooLong, mLineNumber, MAX_LINE_LENGTH)))
        }
        else
            mLine=line.trim()

        val textTags = mutableListOf<TagText>()
        val strippedLine:String

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
                        parser.addError(FileParseError(mLineNumber,mte.message))
                    }
            } else
                end = start + 1
            directiveStart = workLine.indexOf("{", end)
            chordStart = workLine.indexOf("[", end)
        }
        lineOut.append(workLine)
        strippedLine=lineOut.toString()

        // Replace stupid unicode BOM character
        mTaglessLine = strippedLine.replace("\uFEFF", "")
        mTags=parseTags(textTags,parser)
    }

    private fun parseTags(textTags:List<TagText>,parser:TextFileParser<TFileType>):List<Tag>
    {
        return textTags.mapNotNull { tt->
            try {
                parser.parseTag(tt.mText,mLineNumber,tt.mPosition)
            }
            catch(mte:MalformedTagException) {
                parser.addError(FileParseError(mLineNumber,mte.message))
                null
            }
        }
    }

    companion object {
        private const val MAX_LINE_LENGTH = 256
    }
}