package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@NormalizedName(",")
/**
 * Shorthand tag that means "one bar". Multiples of these indicate how many bars the current
 * song file line lasts for.
 */
class BarMarkerTag internal constructor(lineNumber:Int, position:Int): Tag(",",lineNumber,position)