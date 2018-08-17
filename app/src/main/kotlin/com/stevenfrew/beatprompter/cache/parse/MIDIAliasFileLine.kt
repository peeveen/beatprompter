package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

class MIDIAliasFileLine(line:String, lineNumber:Int,  parsingState:MIDIAliasParsingState):FileLine<MIDIAliasParsingState>(line,lineNumber,parsingState) {
    override fun parseTag(text: String, lineNumber: Int, position: Int, parsingState: MIDIAliasParsingState): Tag {
        return Tag.parseMIDIAliasTag(text,lineNumber,position,parsingState)
    }
}