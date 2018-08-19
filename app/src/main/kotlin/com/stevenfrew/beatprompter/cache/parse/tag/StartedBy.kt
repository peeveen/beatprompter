package com.stevenfrew.beatprompter.cache.parse.tag

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class StartedBy constructor(val mStartedBy: KClass<out Tag>)