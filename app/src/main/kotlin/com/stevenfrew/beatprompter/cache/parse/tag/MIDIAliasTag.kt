package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.MIDIAliasParsingState
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasInstructionTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasNameTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasesTag
import java.io.File

class MIDIAliasTag {
    companion object {
        @Throws(MalformedTagException::class)
        fun parse(tagContents: String, lineNumber: Int, position: Int, parsingState: MIDIAliasParsingState): Tag {
            val txt=tagContents.trim('{','}')
            val bits=txt.split(':')
            if(bits.size==2)
            {
                val tagName=bits[0].trim()
                val tagValue=bits[1].trim()
                when(tagName)
                {
                    "midi_aliases"->return MIDIAliasesTag(tagName,lineNumber,position,tagValue)
                    "midi_alias"->return MIDIAliasNameTag(tagName,lineNumber,position,tagValue)
                    else->return MIDIAliasInstructionTag(tagName,lineNumber,position,tagValue)
                }
            }
            else
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.midi_alias_name_contains_more_than_two_parts))
        }
    }
}