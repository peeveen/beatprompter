package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@NormalizedName("filter_only")
class FilterOnlyTag internal constructor(name:String, lineNumber:Int, position:Int): Tag(name,lineNumber,position)