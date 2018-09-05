package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

@OncePerFile
@TagName("key")
/**
 * Tag that defines what key the current song is in.
 */
class KeyTag internal constructor(name:String, lineNumber:Int, position:Int, val mKey:String): ValueTag(name,lineNumber,position,mKey)