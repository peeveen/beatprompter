package com.stevenfrew.beatprompter.cache.parse.tag

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
/**
 * Annotation that tells the parser that this tag starts a section that is ended by another tag
 * type. Allows for generic validation that sections start & end in a nice sequential manner.
 */
annotation class EndedBy(val mEndedBy: KClass<out Tag>)