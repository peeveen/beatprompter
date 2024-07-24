package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Describes a found tag.
 */
data class FoundTag(
	val start: Int,
	val end: Int,
	val name: String,
	val value: String,
	val type: Type
)