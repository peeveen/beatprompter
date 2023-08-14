package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("artist", "a", "subtitle", "st")
@TagType(Type.Directive)
/**
 * Tag that defines the artist or subtitle for a song file.
 */
class ArtistTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	val mArtist: String
) : ValueTag(name, lineNumber, position, mArtist)