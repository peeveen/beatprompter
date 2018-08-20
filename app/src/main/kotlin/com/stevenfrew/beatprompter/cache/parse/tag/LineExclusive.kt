package com.stevenfrew.beatprompter.cache.parse.tag

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class LineExclusive constructor(val mCantShareWith: KClass<out Tag>)