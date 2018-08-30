package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@NormalizedName(",")
class BarMarkerTag internal constructor(lineNumber:Int, position:Int): Tag(",",lineNumber,position)