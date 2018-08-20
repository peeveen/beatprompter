package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

@OncePerFile
@NormalizedName("key")
class KeyTag internal constructor(name:String, lineNumber:Int, position:Int, val mKey:String): ValueTag(name,lineNumber,position,mKey)