package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.*

open class TextFileLine<TFileType>(line:String, val mLineNumber:Int, parser:TextFileParser<TFileType>) {
    private val mLine:String
    val mTaglessLine:String

    val mTags:List<Tag>
    val isEmpty:Boolean
        get()=mLine.isEmpty()

    init
    {
        var currentLine=line.trim()
        if (currentLine.length > MAX_LINE_LENGTH) {
            currentLine = currentLine.substring(0, MAX_LINE_LENGTH)
            parser.addError(FileParseError(mLineNumber,BeatPrompterApplication.getResourceString(R.string.lineTooLong, mLineNumber, MAX_LINE_LENGTH)))
        }

        mLine=currentLine

        val tagCollection=mutableListOf<Tag>()
        while(true) {
            val tagString=parser.findFirstTag(currentLine) ?: break
            val lineWithoutTag=currentLine.substring(0, tagString.mStart)+currentLine.substring(tagString.mEnd+1)
            try {
                val tag=parser.parseTag(tagString,mLineNumber)
                tagCollection.add(tag)
            }
            catch(mte:MalformedTagException) {
                parser.addError(FileParseError(mLineNumber,mte.message!!))
            }
            currentLine=lineWithoutTag.trim()
        }

        mTaglessLine=currentLine.trim()
        mTags=tagCollection
    }

    companion object {
        private const val MAX_LINE_LENGTH = 256
    }
}