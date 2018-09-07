package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
/**
 * Annotation for parser classes. Tells the parser what tags we can ignore. Any tags not
 * found in the parse or ignore lists are reported as errors.
 */
annotation class IgnoreTags constructor(vararg val mTagClasses: KClass<out Tag>)