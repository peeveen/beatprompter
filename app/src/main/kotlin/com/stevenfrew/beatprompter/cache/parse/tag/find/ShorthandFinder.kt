package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Finds shorthand tags, e.g. bar counts and scrollbeat offsets.
 */
object ShorthandFinder
    : TagFinder {
    override fun findTag(text: String): FoundTag? {
        // TODO: dynamic BPB changing with + and _ chars?
        val markerPos =
                if (text.startsWith(','))
                    0
                else if (text.isEmpty())
                    return null
                else {
                    val lastIndex = text.length - 1
                    // Look for the FIRST ending chevron
                    val firstNonChevronIndex = (lastIndex downTo 0).firstOrNull {
                        text[it] != '<' && text[it] != '>'
                    }
                    if (firstNonChevronIndex == null || firstNonChevronIndex == lastIndex)
                        return null
                    else
                        firstNonChevronIndex + 1
                }
        return FoundTag(markerPos,
                markerPos,
                text[markerPos].toString(),
                "",
                Type.Shorthand)
    }
}