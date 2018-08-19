package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

class TagTag internal constructor(name:String,lineNumber:Int,position:Int,val mTag:String): ValueTag(name,lineNumber,position,mTag)