package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Base class for tag finders that find tags that are enclosed in delimiters.
 */
open class EnclosedTagFinder(
	private val mStartChar: Char,
	private val mEndChar: Char,
	private val mTagType: Type,
	private val mRetainCase: Boolean,
	private val mValued: Boolean
) : TagFinder {
	override fun findTag(text: String): FoundTag? =
		text.indexOf(mStartChar).takeIf { it != -1 }?.let { directiveStart ->
			text.indexOf(mEndChar, directiveStart + 1).takeIf { it != -1 }?.let { directiveEnd ->
				text.substring(directiveStart + 1, directiveEnd).trim().let { enclosedText ->
					val (name, value) =
						// Can't use splitAndTrim in case of something like {time:5:00}
						enclosedText.indexOf(":").takeIf { mValued && it != -1 }?.let {
							enclosedText.substring(0, it).trim() to enclosedText.substring(it + 1).trim()
						} ?: (enclosedText to "")

					FoundTag(
						directiveStart,
						directiveEnd,
						if (mRetainCase) name else name.lowercase(),
						value,
						mTagType
					)
				}
			}
		}
}