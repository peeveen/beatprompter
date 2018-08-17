package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.set.SetNameTag

class SetFileLine(line:String, lineNumber:Int, parsingState:SetParsingState):FileLine<SetParsingState>(line,lineNumber,parsingState) {

    init {
        val setNameTag=mTags.filterIsInstance<SetNameTag>().firstOrNull()
        if(setNameTag!=null) {
            if (parsingState.mSetName != null)
                parsingState.mErrors.add(FileParseError(setNameTag, BeatPrompterApplication.getResourceString(R.string.set_name_defined_multiple_times)))
            else
                parsingState.mSetName = setNameTag.mSetName
        }
        else if(!line.isEmpty())
            parsingState.addSongToSet(line)
    }

    override fun parseTag(text: String, lineNumber: Int, position: Int, parsingState: SetParsingState): Tag {
        val txt=text.trim('{','}')
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