package com.stevenfrew.beatprompter.cache.parse.tag.find

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.song.CommentTag
import com.stevenfrew.beatprompter.util.splitAndTrim

/**
 * Finds directive tags, i.e. those that are inside curly brackets.
 */
object DirectiveFinder
    : EnclosedTagFinder('{',
        '}',
        Type.Directive,
        false,
        true) {
    // We made this dumb decision to format the comment tag this way.
    // We're stuck with it. Let's reparse it for the new file parsing system.
    private val mCommentTagNamesWithAudienceMarkers = CommentTag::class
            .annotations
            .asSequence()
            .filterIsInstance<TagName>()
            .map { it.mNames.toList() }
            .flatMap { it.asSequence() }
            .map { it + CommentTag.AUDIENCE_SEPARATOR }
            .toList()

    override fun findTag(text: String): FoundTag? {
        return super.findTag(text)?.let { foundTag ->
            if (mCommentTagNamesWithAudienceMarkers.any { foundTag.mName.startsWith(it) }) {
                val newAudience = foundTag
                        .mName
                        .splitAndTrim(CommentTag.AUDIENCE_SEPARATOR)
                        .asSequence()
                        .drop(1)
                        .joinToString(CommentTag.AUDIENCE_SEPARATOR)
                FoundTag(foundTag.mStart,
                        foundTag.mEnd,
                        "comment",
                        "$newAudience${CommentTag.AUDIENCE_END_MARKER}${foundTag.mValue}",
                        foundTag.mType)
            } else foundTag
        }
    }
}