package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Describes a found tag.
 */
data class FoundTag constructor(val mStart:Int, val mEnd:Int, val mText:String, val mType: TagType)