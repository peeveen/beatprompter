package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@Target(AnnotationTarget.CLASS)
/**
 * Annotation for tag classes. Tells the parser what type of tag it is.
 */
annotation class TagType(val mType: Type)