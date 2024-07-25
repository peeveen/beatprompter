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
	val lineNumber: Int,
	tagParseHelper: TagParsingHelper<TFileType>,
	parser: TextFileParser<TFileType>
) {
	private val line: String
	val lineWithNoTags: String

	val tags: List<Tag>
	val isEmpty: Boolean
		get() = line.isEmpty()

	init {
		var currentLine = line.trim()
		if (currentLine.length > MAX_LINE_LENGTH) {
			currentLine = currentLine.substring(0, MAX_LINE_LENGTH)
			parser.addError(
				FileParseError(
					lineNumber,
					R.string.lineTooLong,
					lineNumber,
					MAX_LINE_LENGTH
				)
			)
		}

		this.line = currentLine

		val tagCollection = mutableListOf<Tag>()
		while (true) {
			val tagString = parser.findFirstTag(currentLine) ?: break
			val lineWithoutTag =
				currentLine.substring(0, tagString.start) + currentLine.substring(tagString.end + 1)
			try {
				val tag = parser.parseTag(tagString, lineNumber, tagParseHelper)
				if (tag != null)
					tagCollection.add(tag)
			} catch (mte: MalformedTagException) {
				parser.addError(FileParseError(lineNumber, mte))
			}
			currentLine = lineWithoutTag.trim()
		}

		lineWithNoTags = currentLine.trim()
		tags = tagCollection
	}

	companion object {
		private const val MAX_LINE_LENGTH = 256
	}
}