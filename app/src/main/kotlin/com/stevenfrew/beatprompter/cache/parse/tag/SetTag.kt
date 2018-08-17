package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.FileParsingState
import com.stevenfrew.beatprompter.cache.parse.SetParsingState
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasInstructionTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasNameTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasesTag
import com.stevenfrew.beatprompter.cache.parse.tag.set.SetNameTag
import java.io.File

class SetTag {
    companion object {
        @Throws(MalformedTagException::class)
        fun parse(tagContents: String, lineNumber: Int, position: Int, parsingState: SetParsingState): Tag {
            val txt=tagContents.trim('{','}')
            val bits=txt.split(':')
            if(bits.size==2)
            {
                val tagName=bits[0].trim()
                val tagValue=bits[1].trim()
                when(tagName)
                {
                    "set"->return SetNameTag(tagName,lineNumber,position,tagValue)
                    else->throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.unexpected_tag_in_setlist_file))
                }
            }
            else
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.midi_alias_name_contains_more_than_two_parts))
        }

    }
}