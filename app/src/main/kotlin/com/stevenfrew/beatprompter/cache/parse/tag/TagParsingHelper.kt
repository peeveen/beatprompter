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
class TagParsingHelper<FileResultType> constructor(parser: TextFileParser<FileResultType>) {
    val mNameToClassMap: Map<Pair<Type, String>, KClass<out Tag>>
    val mNoNameToClassMap: Map<Type, KClass<out Tag>>
    val mIgnoreTagNames: List<String>

    init {
        val parseClasses = parser::class.annotations.filterIsInstance<ParseTags>().flatMap { it.mTagClasses.toList() }
        val unnamedAndNamedParseClasses = parseClasses.partition { it.annotations.filterIsInstance<TagName>().isEmpty() }
        val unnamedParseClasses = unnamedAndNamedParseClasses.first
        val namedParseClasses = unnamedAndNamedParseClasses.second
        mNameToClassMap = namedParseClasses.flatMap { tagClass -> tagClass.annotations.filterIsInstance<TagName>().flatMap { it.mNames.map { tagName -> Pair(Pair(tagClass.findAnnotation<TagType>()!!.mType, tagName), tagClass) } } }.toMap()
        mNoNameToClassMap = unnamedParseClasses.map { tagClass -> Pair(tagClass.findAnnotation<TagType>()!!.mType, tagClass) }.toMap()
        mIgnoreTagNames = parser::class.annotations.filterIsInstance<IgnoreTags>().flatMap { ignoreAnnotation -> ignoreAnnotation.mTagClasses.flatMap { tagClass -> tagClass.annotations.filterIsInstance<TagName>().flatMap { it.mNames.toList() } } }
    }
}