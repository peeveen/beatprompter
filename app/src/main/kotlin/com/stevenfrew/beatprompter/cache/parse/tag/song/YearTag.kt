package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("year", "y")
@TagType(Type.Directive)
/**
 * Tag that defines the year of release of this song.
 */
class YearTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	year: String
) : ValueTag(name, lineNumber, position, year) {
	val year = TagParsingUtility.parseIntegerValue(year, 0, 9999)
}