package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import kotlin.reflect.KClass

/**
 * Collects all tag parsing metadata into one place (well, three places).
 * We could do all this work each time we reach a tag, but this saves a lot of processing.
 */
class TagParsingHelper<FileResultType> constructor(parser:TextFileParser<FileResultType>)
{
    val mNameToClassMap:Map<Pair<Type,String>, KClass<out Tag>>
    val mNoNameToClassMap:Map<Type,KClass<out Tag>>
    val mIgnoreTagNames:List<String>
    init {
        val parseClasses=parser::class.annotations.filterIsInstance<ParseTags>().flatMap{it.mTagClasses.toList()}
        val unnamedAndNamedParseClasses=parseClasses.partition { it.annotations.filterIsInstance<TagName>().isEmpty() }
        val unnamedParseClasses=unnamedAndNamedParseClasses.first
        val namedParseClasses=unnamedAndNamedParseClasses.second
        mNameToClassMap=namedParseClasses.flatMap{tagClass-> tagClass.annotations.filterIsInstance<TagName>().flatMap{it.mNames.map{ tagName->Pair(Pair(tagClass.annotations.filterIsInstance<TagType>().first().mType,tagName),tagClass)}}}.toMap()
        mNoNameToClassMap=unnamedParseClasses.map{tagClass-> Pair(tagClass.annotations.filterIsInstance<TagType>().first().mType,tagClass)}.toMap()
        mIgnoreTagNames=parser::class.annotations.filterIsInstance<IgnoreTags>().flatMap{ignoreAnnotation->ignoreAnnotation.mTagClasses.flatMap{tagClass->tagClass.annotations.filterIsInstance<TagName>().flatMap{it.mNames.toList()}}}
    }
}