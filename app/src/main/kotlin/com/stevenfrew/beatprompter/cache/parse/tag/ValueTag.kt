package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R

open class ValueTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): Tag(name,lineNumber,position)
{
    init {
        if(value.isBlank())
            throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.tag_has_blank_value,name))
    }
}
