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
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * Base class for text file parsers.
 */
abstract class TextFileParser<TFileResult>(
	cachedCloudFile: CachedFile,
	private val mReportUnexpectedTags: Boolean,
	private vararg val mTagFinders: TagFinder
) : FileParser<TFileResult>(cachedCloudFile) {
	final override fun parse(): TFileResult {
		val tagParseHelper = TagParsingUtility.getTagParsingHelper(this)
		var lineNumber = 0
		val fileTags = mutableSetOf<KClass<out Tag>>()
		val livePairings = mutableSetOf<Pair<KClass<out Tag>, KClass<out Tag>>>()
		mCachedCloudFile.mFile.forEachLine { strLine ->
			++lineNumber
			val txt = strLine.trim().removeControlCharacters()
			// Ignore empty lines and comments
			if (txt.isNotEmpty() && !txt.startsWith('#')) {
				val textLine = TextFileLine(txt, lineNumber, tagParseHelper, this)
				val lineTags = mutableSetOf<KClass<out Tag>>()
				textLine.mTags.forEach { tag ->
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
						mErrors.add(FileParseError(tag, R.string.tag_used_multiple_times_in_file, tag.mName))
					if (isOncePerLine && alreadyUsedInLine)
						mErrors.add(FileParseError(tag, R.string.tag_used_multiple_times_in_line, tag.mName))
					if (startedByAnnotation != null) {
						val startedByClass = startedByAnnotation.mStartedBy
						val startedByEndedByAnnotation = startedByClass.findAnnotation<EndedBy>()!!
						val endedByClass = startedByEndedByAnnotation.mEndedBy
						if (!livePairings.remove(startedByClass to endedByClass))
							mErrors.add(
								FileParseError(
									tag,
									R.string.ending_tag_found_before_starting_tag,
									tag.mName
								)
							)
					} else if (endedByAnnotation != null) {
						val endedByClass = endedByAnnotation.mEndedBy
						val endedByStartedByAnnotation = endedByClass.findAnnotation<StartedBy>()!!
						val startedByClass = endedByStartedByAnnotation.mStartedBy
						val pairing = startedByClass to endedByClass
						if (livePairings.contains(pairing))
							mErrors.add(
								FileParseError(
									tag,
									R.string.starting_tag_found_after_starting_tag,
									tag.mName
								)
							)
						else
							livePairings.add(pairing)
					}
					lineExclusiveTags.forEach {
						if (lineTags.contains(it.mCantShareWith))
							mErrors.add(
								FileParseError(
									tag, R.string.tag_cant_share_line_with,
									tag.mName,
									it.mCantShareWith.findAnnotation<TagName>()!!.mNames.first()
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
		if (!parseHelper.mIgnoreTagNames.contains(foundTag.mName)) {
			// Nope, better parse it!
			val tagClass = parseHelper.mNameToClassMap[foundTag.mType to foundTag.mName]
				?: parseHelper.mNoNameToClassMap[foundTag.mType]
			if (tagClass != null) {
				// We found a match!
				// Construct a tag of this class
				try {
					tagClass.primaryConstructor?.apply {
						return if (parameters.size == 4)
							call(foundTag.mName, lineNumber, foundTag.mStart, foundTag.mValue)
						else
							call(foundTag.mName, lineNumber, foundTag.mStart)
					}
				} catch (ite: InvocationTargetException) {
					throw ite.targetException
				}
			} else if (mReportUnexpectedTags)
				throw MalformedTagException(R.string.unexpected_tag_in_file, foundTag.mName)
		}
		return null
	}

	fun findFirstTag(text: String): FoundTag? {
		return mTagFinders
			.mapNotNull { it.findTag(text) }
			.minByOrNull { it.mStart }
	}
}