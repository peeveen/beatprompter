package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Describes a found tag.
 */
data class FoundTag constructor(val mStart:Int, val mEnd:Int, val mName:String, val mValue:String, val mType: Type)