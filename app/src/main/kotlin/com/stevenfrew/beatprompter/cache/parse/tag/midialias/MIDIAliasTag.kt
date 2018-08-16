package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException

class MIDIAliasTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): MIDITag(name,lineNumber,position) {
    val mAliasName:String

    init {
        if (value.contains(":"))
            throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.midi_alias_name_contains_more_than_two_parts))
        if (value.isEmpty())
            throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.midi_alias_without_a_name))
        mAliasName=value
    }
}


