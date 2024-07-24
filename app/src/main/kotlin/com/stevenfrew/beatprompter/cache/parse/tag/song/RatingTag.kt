package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("rating", "r")
@TagType(Type.Directive)
/**
 * Tag that defines the rating of this song (1-5, integer).
 */
class RatingTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	rating: String
) : ValueTag(name, lineNumber, position, rating) {
	val rating = TagParsingUtility.parseIntegerValue(rating, 1, 5)
}