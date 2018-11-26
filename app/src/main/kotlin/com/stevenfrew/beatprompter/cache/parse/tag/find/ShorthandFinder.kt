package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Finds shorthand tags, e.g. bar counts and scrollbeat offsets.
 */
object ShorthandFinder : TagFinder {
    override fun findTag(text: String): FoundTag? {
        // TODO: dynamic BPB changing with + and _ chars?
        val markerPos =
                if (text.startsWith(','))
                    0
                else if (text.isEmpty())
                    -1
                else {
                    // Look for the FIRST ending chevron
                    var position = text.length - 1
                    while (position >= 0 && (text[position] == '<' || text[position] == '>'))
                        --position
                    if (++position < text.length)
                        position
                    else
                        -1
                }
        if (markerPos == -1)
            return null
        return FoundTag(markerPos, markerPos, text[markerPos].toString(), "", Type.Shorthand)
    }
}