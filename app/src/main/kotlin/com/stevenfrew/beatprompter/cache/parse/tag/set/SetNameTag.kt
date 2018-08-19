package com.stevenfrew.beatprompter.cache.parse.tag.set

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@OncePerFile
class SetNameTag internal constructor(name:String, lineNumber:Int, position:Int, val mSetName:String): Tag(name,lineNumber,position)