package com.stevenfrew.beatprompter.cache.parse.tag

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
/**
 * Annotation that tells the parser that this tag cannot share a line with the named tag.
 */
annotation class LineExclusive(val cannotShareWith: KClass<out Tag>)