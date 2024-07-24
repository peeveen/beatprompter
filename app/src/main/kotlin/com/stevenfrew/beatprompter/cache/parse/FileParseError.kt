package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

/**
 * A description of a parsing error that can be shown to the user.
 */
class FileParseError private constructor(val lineNumber: Int, val message: String) {

	internal constructor(lineNumber: Int, resourceId: Int, vararg args: Any)
		: this(lineNumber, BeatPrompter.appResources.getString(resourceId, *args))

	internal constructor(tag: Tag, t: Throwable)
		: this(tag.lineNumber, t.message ?: t.toString())

	internal constructor(lineNumber: Int, t: Throwable)
		: this(lineNumber, t.message ?: t.toString())

	internal constructor(tag: Tag, resourceId: Int, vararg args: Any)
		: this(tag.lineNumber, resourceId, *args)

	internal constructor(resourceId: Int, vararg args: Any)
		: this(-1, resourceId, *args)

	override fun toString(): String = (if (lineNumber != -1) "$lineNumber: " else "") + message

	override fun hashCode(): Int = 31 * lineNumber.hashCode() + message.hashCode()

	override fun equals(other: Any?): Boolean =
		if (this === other) true
		else if (javaClass != other?.javaClass) false
		else (other as FileParseError).let {
			if (message != other.message)
				false
			else if (lineNumber != other.lineNumber)
				false
			else
				true
		}
}