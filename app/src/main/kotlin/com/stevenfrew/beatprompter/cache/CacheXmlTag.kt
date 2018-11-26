package com.stevenfrew.beatprompter.cache

@Target(AnnotationTarget.CLASS)
/**
 * Annotation that tells the parser what XML tags describe what filetypes.
 */
annotation class CacheXmlTag constructor(val mTag: String)