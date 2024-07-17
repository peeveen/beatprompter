package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Finds shorthand tags, e.g. bar counts and scrollbeat offsets.
 */
object ShorthandFinder
	: TagFinder {
	override fun findTag(text: String): FoundTag? =
		// TODO: dynamic BPB changing with + and _ chars?
		when {
			text.startsWith(',') -> 0
			text.isEmpty() -> null
			else -> {
				val lastIndex = text.length - 1
				// Look for the FIRST ending chevron
				when (val firstNonChevronIndex = (lastIndex downTo 0).firstOrNull {
					text[it] != '<' && text[it] != '>'
				}) {
					// Entire string was chevrons
					null -> 0
					// Last character was NOT a chevron
					lastIndex -> null
					// Normal scenario
					else -> firstNonChevronIndex + 1
				}
			}
		}?.let {
			FoundTag(
				it,
				it,
				text[it].toString(),
				"",
				Type.Shorthand
			)
		}
}
