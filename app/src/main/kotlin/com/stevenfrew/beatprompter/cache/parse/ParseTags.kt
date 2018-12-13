package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
/**
 * Annotation for parser classes. Tells the parser what tags we are interested in parsing.
 * Any tags that are not in the parse or ignore lists will be reported as errors.
 */
annotation class ParseTags(vararg val mTagClasses: KClass<out Tag>)