package com.stevenfrew.beatprompter.cache.parse.tag.find

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.song.CommentTag
import com.stevenfrew.beatprompter.util.splitAndTrim

/**
 * Finds directive tags, i.e. those that are inside curly brackets.
 */
object DirectiveFinder
	: EnclosedTagFinder(
	'{',
	'}',
	Type.Directive,
	false,
	true
) {
	// We made this dumb decision to format the comment tag this way.
	// We're stuck with it. Let's reparse it for the new file parsing system.
	private val commentTagNamesWithAudienceMarkers = CommentTag::class
		.annotations
		.asSequence()
		.filterIsInstance<TagName>()
		.map { it.names.toList() }
		.flatMap { it.asSequence() }
		.map { "$it${CommentTag.AUDIENCE_SEPARATOR}" }
		.toList()

	override fun findTag(text: String): FoundTag? =
		super.findTag(text)?.let { foundTag ->
			if (commentTagNamesWithAudienceMarkers.any { foundTag.name.startsWith(it) }) {
				val newAudience = foundTag
					.name
					.splitAndTrim(CommentTag.AUDIENCE_SEPARATOR)
					.asSequence()
					.drop(1)
					.joinToString(CommentTag.AUDIENCE_SEPARATOR)
				FoundTag(
					foundTag.start,
					foundTag.end,
					"comment",
					"$newAudience${CommentTag.AUDIENCE_END_MARKER}${foundTag.value}",
					foundTag.type
				)
			} else foundTag
		}
}
