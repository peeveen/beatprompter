package com.stevenfrew.beatprompter.cache.parse.tag

@Target(AnnotationTarget.CLASS)
annotation class TagName constructor(vararg val mNames:String)