package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.parse.tag.MIDIAliasTag
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import java.io.File

class MIDIAliasFileLine(line:String, lineNumber:Int,  parsingState:MIDIAliasParsingState):FileLine<MIDIAliasParsingState>(line,lineNumber,parsingState) {
    override fun parseTag(text: String, lineNumber: Int, position: Int, parsingState: MIDIAliasParsingState): Tag {
        return MIDIAliasTag.parse(text,lineNumber,position,parsingState)
    }
}