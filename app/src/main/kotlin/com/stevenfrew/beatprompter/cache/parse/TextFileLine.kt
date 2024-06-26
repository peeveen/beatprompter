package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingHelper

/**
 * Represents a line from a parsed text file.
 */
open class TextFileLine<TFileType>(
	line: String,
	val mLineNumber: Int,
	tagParseHelper: TagParsingHelper<TFileType>,
	parser: TextFileParser<TFileType>
) {
	private val mLine: String
	val mLineWithNoTags: String

	val mTags: List<Tag>
	val isEmpty: Boolean
		get() = mLine.isEmpty()

	init {
		var currentLine = line.trim()
		if (currentLine.length > MAX_LINE_LENGTH) {
			currentLine = currentLine.substring(0, MAX_LINE_LENGTH)
			parser.addError(
				FileParseError(
					mLineNumber,
					R.string.lineTooLong,
					mLineNumber,
					MAX_LINE_LENGTH
				)
			)
		}

		mLine = currentLine

		val tagCollection = mutableListOf<Tag>()
		while (true) {
			val tagString = parser.findFirstTag(currentLine) ?: break
			val lineWithoutTag =
				currentLine.substring(0, tagString.mStart) + currentLine.substring(tagString.mEnd + 1)
			try {
				val tag = parser.parseTag(tagString, mLineNumber, tagParseHelper)
				if (tag != null)
					tagCollection.add(tag)
			} catch (mte: MalformedTagException) {
				parser.addError(FileParseError(mLineNumber, mte))
			}
			currentLine = lineWithoutTag.trim()
		}

		mLineWithNoTags = currentLine.trim()
		mTags = tagCollection
	}

	companion object {
		private const val MAX_LINE_LENGTH = 256
	}
}