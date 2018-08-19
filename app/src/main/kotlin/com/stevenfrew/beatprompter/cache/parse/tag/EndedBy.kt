package com.stevenfrew.beatprompter.cache.parse.tag

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class EndedBy constructor(val mEndedBy: KClass<out Tag>)