package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

@OncePerFile
@TagName("title","t")
/**
 * Tag that defines the title of this song.
 */
class TitleTag internal constructor(name:String, lineNumber:Int, position:Int, val mTitle:String): ValueTag(name,lineNumber,position,mTitle)