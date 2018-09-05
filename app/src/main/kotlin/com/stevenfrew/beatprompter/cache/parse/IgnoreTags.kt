package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class IgnoreTags constructor(vararg val mTagClasses: KClass<out Tag>)