package com.stevenfrew.beatprompter.cache.parse.tag.find

object DirectiveFinder: EnclosedTagFinder('{','}', TagType.Directive)