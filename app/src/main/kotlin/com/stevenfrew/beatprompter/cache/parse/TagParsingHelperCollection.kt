package com.stevenfrew.beatprompter.cache.parse

import kotlin.reflect.KClass

object TagParsingHelperCollection {
    private val mHelperMap=mutableMapOf<KClass<out Any>,TagParsingHelper<Any>>()
    fun <T> getTagParsingHelper(parser:TextFileParser<T>):TagParsingHelper<T>
    {
        return mHelperMap.getOrPut(parser::class) {TagParsingHelper(parser) as TagParsingHelper<Any>} as TagParsingHelper<T>
    }
}