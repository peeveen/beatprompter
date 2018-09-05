package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class ParseTags constructor(vararg val mTagClasses: KClass<out Tag>)