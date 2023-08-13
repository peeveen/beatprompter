package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.util.Utils

/**
 * Base class for tags that define a color for some purpose.
 */
open class ColorTag protected constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : ValueTag(name, lineNumber, position, value) {
	val mColor = Utils.makeHighlightColour(TagParsingUtility.parseColourValue(value))
}