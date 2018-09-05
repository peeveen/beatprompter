package com.stevenfrew.beatprompter.cache.parse.tag.set

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@OncePerFile
@TagName("set")
/**
 * Tag that defines the name of a setlist.
 */
class SetNameTag internal constructor(name:String, lineNumber:Int, position:Int, val mSetName:String): Tag(name,lineNumber,position)