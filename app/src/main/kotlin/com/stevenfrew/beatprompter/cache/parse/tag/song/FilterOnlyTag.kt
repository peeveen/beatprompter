package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

@TagName("filter_only")
/**
 * Tag that instructs the app to NOT list this song file in the main song list. It will only be
 * shown when a relevant tag or subfolder filter is selected.
 */
class FilterOnlyTag internal constructor(name:String, lineNumber:Int, position:Int): Tag(name,lineNumber,position)