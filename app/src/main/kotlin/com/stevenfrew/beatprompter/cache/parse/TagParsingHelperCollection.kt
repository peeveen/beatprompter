package com.stevenfrew.beatprompter.cache.parse

import kotlin.reflect.KClass

/**
 * Singleton map of parser-type to TagParsingHelper. Saves a lot of annotation processing.
 * Should only construct one TagParsingHelper per file type, instead of one per file.
 */
object TagParsingHelperCollection {
    private val mHelperMap=mutableMapOf<KClass<out Any>,TagParsingHelper<Any>>()
    fun <T> getTagParsingHelper(parser:TextFileParser<T>):TagParsingHelper<T>
    {
        return mHelperMap.getOrPut(parser::class) {TagParsingHelper(parser) as TagParsingHelper<Any>} as TagParsingHelper<T>
    }
}