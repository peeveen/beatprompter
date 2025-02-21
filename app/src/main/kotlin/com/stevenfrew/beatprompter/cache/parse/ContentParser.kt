package com.stevenfrew.beatprompter.cache.parse

import org.w3c.dom.Element

/**
 * Interface for all content parsers.
 */
interface ContentParser<TResult> {
	fun parse(element: Element? = null): TResult
}