package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.parse.tag.SetTag
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import java.io.File

class SetFileLine(line:String, lineNumber:Int, parsingState:SetParsingState):FileLine<SetParsingState>(line,lineNumber,parsingState) {
    override fun parseTag(text: String, lineNumber: Int, position: Int, parsingState: SetParsingState): Tag {
        return SetTag.parse(text,lineNumber,position,parsingState)
    }
}