package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasInstructionTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasNameTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasSetNameTag

class MIDIAliasFileLine(line:String, lineNumber:Int,  parsingState:MIDIAliasParsingState):FileLine<MIDIAliasParsingState>(line,lineNumber,parsingState) {

    init {
        val midiAliasSetNameTag=mTags.filterIsInstance<MIDIAliasSetNameTag>().firstOrNull()
        if(midiAliasSetNameTag!=null)
            if(parsingState.mAliasSetName!=null)
                parsingState.mErrors.add(FileParseError(midiAliasSetNameTag,BeatPrompterApplication.getResourceString(R.string.midi_alias_set_name_defined_multiple_times)))
            else
                parsingState.mAliasSetName=midiAliasSetNameTag.mAliasSetName

        val midiAliasNameTag=mTags.filterIsInstance<MIDIAliasNameTag>().firstOrNull()
        if(midiAliasNameTag!=null)
            parsingState.startNewAlias(midiAliasNameTag.mAliasName)

        val midiAliasInstructionTag=mTags.filterIsInstance<MIDIAliasInstructionTag>().firstOrNull()
        if(midiAliasInstructionTag!=null)
            parsingState.addInstructionToCurrentAlias(midiAliasInstructionTag.mInstructions)
    }

    override fun parseTag(text: String, lineNumber: Int, position: Int, parsingState: MIDIAliasParsingState): Tag {
        val txt=text.trim('{','}')
        val bits=txt.split(':')
        return if(bits.size==2) {
            val tagName=bits[0].trim()
            val tagValue=bits[1].trim()
            when(tagName) {
                "midi_aliases"-> MIDIAliasSetNameTag(tagName,lineNumber,position,tagValue)
                "midi_alias"-> MIDIAliasNameTag(tagName,lineNumber,position,tagValue)
                else-> MIDIAliasInstructionTag(tagName,lineNumber,position,tagValue)
            }
        } else
            throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.midi_alias_name_contains_more_than_two_parts))
    }
}