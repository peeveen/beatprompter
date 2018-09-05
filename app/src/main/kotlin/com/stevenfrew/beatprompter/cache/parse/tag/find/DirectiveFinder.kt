package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Finds directive tags, i.e. those that are inside curly brackets.
 */
object DirectiveFinder: EnclosedTagFinder('{','}', TagType.Directive)