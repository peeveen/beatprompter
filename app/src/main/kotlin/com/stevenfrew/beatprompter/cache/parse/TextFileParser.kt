package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.parse.tag.EndedBy
import com.stevenfrew.beatprompter.cache.parse.tag.LineExclusive
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.StartedBy
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingHelper
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.find.FoundTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.TagFinder
import com.stevenfrew.beatprompter.util.removeControlCharacters
import org.w3c.dom.Element
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * Base class for text file parsers.
 */
abstract class TextFileParser<TFileResult>(
	cachedCloudFile: CachedFile,
	private val reportUnexpectedTags: Boolean,
	private vararg val tagFinders: TagFinder
) : FileParser<TFileResult>(cachedCloudFile) {
	override fun parse(element: Element?): TFileResult {
		val tagParseHelper = TagParsingUtility.getTagParsingHelper(this)
		var lineNumber = 0
		val fileTags = mutableSetOf<KClass<out Tag>>()
		val livePairings = mutableSetOf<Pair<KClass<out Tag>, KClass<out Tag>>>()
		cachedCloudFile.file.forEachLine { strLine ->
			++lineNumber
			val txt = strLine.trim().removeControlCharacters()
			// Ignore empty lines and comments
			if (txt.isNotEmpty() && !txt.startsWith('#')) {
				val textLine = TextFileLine(txt, lineNumber, tagParseHelper, this)
				val lineTags = mutableSetOf<KClass<out Tag>>()
				textLine.tags.forEach { tag ->
					val tagClass = tag::class
					val isOncePerFile = tagClass.findAnnotation<OncePerFile>() != null
					val isOncePerLine = tagClass.findAnnotation<OncePerLine>() != null
					val startedByAnnotation = tagClass.findAnnotation<StartedBy>()
					val endedByAnnotation = tagClass.findAnnotation<EndedBy>()
					val lineExclusiveTags = tagClass.annotations.filterIsInstance<LineExclusive>()
					val alreadyUsedInFile = fileTags.contains(tagClass)
					val alreadyUsedInLine = lineTags.contains(tagClass)
					fileTags.add(tagClass)
					lineTags.add(tagClass)
					if (isOncePerFile && alreadyUsedInFile)
						errors.add(FileParseError(tag, R.string.tag_used_multiple_times_in_file, tag.name))
					if (isOncePerLine && alreadyUsedInLine)
						errors.add(FileParseError(tag, R.string.tag_used_multiple_times_in_line, tag.name))
					if (startedByAnnotation != null) {
						val startedByClass = startedByAnnotation.startedBy
						val startedByEndedByAnnotation = startedByClass.findAnnotation<EndedBy>()!!
						val endedByClass = startedByEndedByAnnotation.endedBy
						if (!livePairings.remove(startedByClass to endedByClass))
							errors.add(
								FileParseError(
									tag,
									R.string.ending_tag_found_before_starting_tag,
									tag.name
								)
							)
					} else if (endedByAnnotation != null) {
						val endedByClass = endedByAnnotation.endedBy
						val endedByStartedByAnnotation = endedByClass.findAnnotation<StartedBy>()!!
						val startedByClass = endedByStartedByAnnotation.startedBy
						val pairing = startedByClass to endedByClass
						if (livePairings.contains(pairing))
							errors.add(
								FileParseError(
									tag,
									R.string.starting_tag_found_after_starting_tag,
									tag.name
								)
							)
						else
							livePairings.add(pairing)
					}
					lineExclusiveTags.forEach {
						if (lineTags.contains(it.cannotShareWith))
							errors.add(
								FileParseError(
									tag, R.string.tag_cant_share_line_with,
									tag.name,
									it.cannotShareWith.findAnnotation<TagName>()!!.names.first()
								)
							)
					}
				}
				parseLine(textLine)
			}
		}
		return getResult()
	}

	abstract fun getResult(): TFileResult

	abstract fun parseLine(line: TextFileLine<TFileResult>)

	fun parseTag(
		foundTag: FoundTag,
		lineNumber: Int,
		parseHelper: TagParsingHelper<TFileResult>
	): Tag? {
		// Should we ignore this tag?
		if (!parseHelper.ignoreTagNames.contains(foundTag.name)) {
			// Nope, better parse it!
			val tagClass = parseHelper.nameToClassMap[foundTag.type to foundTag.name]
				?: parseHelper.noNameToClassMap[foundTag.type]
			if (tagClass != null) {
				// We found a match!
				// Construct a tag of this class
				try {
					tagClass.primaryConstructor?.apply {
						return if (parameters.size == 4)
							call(foundTag.name, lineNumber, foundTag.start, foundTag.value)
						else
							call(foundTag.name, lineNumber, foundTag.start)
					}
				} catch (ite: InvocationTargetException) {
					throw ite.targetException
				}
			} else if (reportUnexpectedTags)
				throw MalformedTagException(R.string.unexpected_tag_in_file, foundTag.name)
		}
		return null
	}

	fun findFirstTag(text: String): FoundTag? =
		tagFinders
			.mapNotNull { it.findTag(text) }
			.minByOrNull { it.start }
}