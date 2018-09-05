package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

/**
 * Tag that isn't used in the current context.
 */
class UnusedTag internal constructor(name:String, lineNumber:Int, position:Int): Tag(name,lineNumber,position)