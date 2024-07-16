package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.cache.parse.IgnoreTags
import com.stevenfrew.beatprompter.cache.parse.ParseTags
import com.stevenfrew.beatprompter.cache.parse.TextFileParser
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Collects all tag parsing metadata into one place (well, three places).
 * We could do all this work each time we reach a tag, but this saves a lot of processing.
 */
class TagParsingHelper<FileResultType>(parser: TextFileParser<FileResultType>) {
	val mNameToClassMap: Map<Pair<Type, String>, KClass<out Tag>>
	val mNoNameToClassMap: Map<Type, KClass<out Tag>>
	val mIgnoreTagNames: List<String>

	init {
		val (unnamedParseClasses, namedParseClasses) = parser::class
			.annotations
			.filterIsInstance<ParseTags>()
			.flatMap { it.mTagClasses.toList() }
			.partition { it.annotations.filterIsInstance<TagName>().isEmpty() }
		mNameToClassMap = namedParseClasses.flatMap { tagClass ->
			tagClass
				.annotations
				.filterIsInstance<TagName>()
				.flatMap {
					it
						.mNames
						.map { tagName ->
							(tagClass.findAnnotation<TagType>()!!.mType to tagName) to tagClass
						}
				}
		}.toMap()
		mNoNameToClassMap = unnamedParseClasses.associateBy { tagClass ->
			tagClass.findAnnotation<TagType>()!!.mType
		}
		mIgnoreTagNames = parser::class
			.annotations
			.filterIsInstance<IgnoreTags>()
			.flatMap { ignoreAnnotation ->
				ignoreAnnotation
					.mTagClasses
					.flatMap { tagClass ->
						tagClass
							.annotations
							.filterIsInstance<TagName>()
							.flatMap { it.mNames.toList() }
					}
			}
	}
}