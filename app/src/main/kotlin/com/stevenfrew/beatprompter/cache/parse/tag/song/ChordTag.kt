package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@TagType(Type.Chord)
/**
 * Tag that defines a chord to be displayed at this point.
 */
class ChordTag(
	chordText: String,
	lineNumber: Int,
	position: Int
) : Tag(chordText, lineNumber, position)