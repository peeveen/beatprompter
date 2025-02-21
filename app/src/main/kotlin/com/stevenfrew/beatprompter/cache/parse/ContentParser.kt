package com.stevenfrew.beatprompter.cache.parse

import org.w3c.dom.Element

/**
 * Base class for all file parsers.
 */
abstract class ContentParser<TResult> {
	private val errorList = mutableListOf<ContentParsingError>()
	val errors: List<ContentParsingError> get() = errorList

	abstract fun parse(element: Element? = null): TResult

	fun addError(error: ContentParsingError) = errorList.add(error)
}