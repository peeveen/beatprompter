package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

@TagName("tag")
/**
 * Tag that defines a category that this song will be grouped under in the filter menu.
 */
class TagTag internal constructor(name:String,lineNumber:Int,position:Int,val mTag:String): ValueTag(name,lineNumber,position,mTag)