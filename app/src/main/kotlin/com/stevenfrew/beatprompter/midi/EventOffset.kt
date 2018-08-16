package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

class EventOffset constructor(val mAmount:Int,val mOffsetType:EventOffsetType,val mSourceTag:Tag?=null){
    companion object
    {
        val NoOffset=EventOffset(0,EventOffsetType.Milliseconds)
    }
}