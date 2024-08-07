package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.R

/**
 * Base class for tags that have a value.
 */
open class ValueTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : Tag(name, lineNumber, position) {
	init {
		value.ifBlank { throw MalformedTagException(R.string.tag_has_blank_value, name) }
	}
}
