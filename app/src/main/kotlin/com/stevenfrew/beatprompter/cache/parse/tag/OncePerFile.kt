package com.stevenfrew.beatprompter.cache.parse.tag

@Target(AnnotationTarget.CLASS)
/**
 * Annotation for a tag class that declares that it should only exist once in a file.
 */
annotation class OncePerFile