package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Interface for tag finders.
 */
interface TagFinder {
    fun findTag(text:String): FoundTag?
}