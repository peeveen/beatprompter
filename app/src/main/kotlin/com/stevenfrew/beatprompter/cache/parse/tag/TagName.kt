package com.stevenfrew.beatprompter.cache.parse.tag

@Target(AnnotationTarget.CLASS)
/**
 * Annotation for tag classes. Tells the parser all possible synonymous tag names that
 * are related to this tag.
 */
annotation class TagName(vararg val mNames: String)