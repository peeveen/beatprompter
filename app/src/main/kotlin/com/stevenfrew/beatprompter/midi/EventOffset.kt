package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

data class EventOffset constructor(val mAmount:Int,val mOffsetType:EventOffsetType,val mSourceTag:Tag)