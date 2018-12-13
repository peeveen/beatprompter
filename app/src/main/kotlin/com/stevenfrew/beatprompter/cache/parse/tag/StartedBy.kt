package com.stevenfrew.beatprompter.cache.parse.tag

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
/**
 * Annotation for a tag class that ends a defined section. This tells the parser what tag
 * indicates the START of the defined section. Allows for generic validation of sequential
 * start/stops.
 */
annotation class StartedBy(val mStartedBy: KClass<out Tag>)