package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.util.splitAndTrim

abstract class StartOfVariationTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : Tag(
	name,
	lineNumber,
	position,
) {
	val variations: List<String> = value.splitAndTrim(",").filter { it.isNotBlank() }
}