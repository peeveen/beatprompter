package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@Target(AnnotationTarget.CLASS)
annotation class TagType constructor(val mType:Type)