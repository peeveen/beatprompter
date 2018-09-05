package com.stevenfrew.beatprompter.cache.parse.tag.find

import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.song.CommentTag
import com.stevenfrew.beatprompter.splitAndTrim

/**
 * Finds directive tags, i.e. those that are inside curly brackets.
 */
object DirectiveFinder: EnclosedTagFinder('{','}', Type.Directive, true)
{
    // We made this dumb decision to format the comment tag this way.
    // We're stuck with it. Let's reparse it for the new file parsing system.
    private val mCommentTagNamesWithAudienceMarkers:List<String>
    init
    {
        val commentTagNames=CommentTag::class.annotations.filterIsInstance<TagName>().map{it.mNames.toList()}.flatMap { it }
        mCommentTagNamesWithAudienceMarkers=commentTagNames.map{it+CommentTag.AUDIENCE_SEPARATOR}
    }

    override fun findTag(text: String): FoundTag? {
        val result=super.findTag(text)
        if(result!=null)
        {
            if(mCommentTagNamesWithAudienceMarkers.any{result.mName.startsWith(it)})
            {
                val bits=result.mName.splitAndTrim(CommentTag.AUDIENCE_SEPARATOR)
                val newAudience=bits.drop(1).joinToString(CommentTag.AUDIENCE_SEPARATOR)
                return FoundTag(result.mStart,result.mEnd,"comment",newAudience+CommentTag.AUDIENCE_END_MARKER+result.mValue,result.mType)
            }
        }
        return result
    }
}