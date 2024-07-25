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
	val nameToClassMap: Map<Pair<Type, String>, KClass<out Tag>>
	val noNameToClassMap: Map<Type, KClass<out Tag>>
	val ignoreTagNames: List<String>

	init {
		val (unnamedParseClasses, namedParseClasses) = parser::class
			.annotations
			.filterIsInstance<ParseTags>()
			.flatMap { it.tagClasses.toList() }
			.partition { it.annotations.filterIsInstance<TagName>().isEmpty() }
		nameToClassMap = namedParseClasses.flatMap { tagClass ->
			tagClass
				.annotations
				.filterIsInstance<TagName>()
				.flatMap {
					it
						.names
						.map { tagName ->
							(tagClass.findAnnotation<TagType>()!!.type to tagName) to tagClass
						}
				}
		}.toMap()
		noNameToClassMap = unnamedParseClasses.associateBy { tagClass ->
			tagClass.findAnnotation<TagType>()!!.type
		}
		ignoreTagNames = parser::class
			.annotations
			.filterIsInstance<IgnoreTags>()
			.flatMap { ignoreAnnotation ->
				ignoreAnnotation
					.tagClasses
					.flatMap { tagClass ->
						tagClass
							.annotations
							.filterIsInstance<TagName>()
							.flatMap { it.names.toList() }
					}
			}
	}
}