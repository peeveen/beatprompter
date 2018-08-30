package com.stevenfrew.beatprompter.cache.parse.tag.find

interface TagFinder {
    fun findTag(text:String): FoundTag?
}