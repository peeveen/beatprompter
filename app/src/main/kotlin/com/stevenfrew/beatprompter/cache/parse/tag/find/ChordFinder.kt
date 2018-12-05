package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Finds chord tags, i.e. those that are inside square brackets.
 */
object ChordFinder
    : EnclosedTagFinder('[',
        ']',
        Type.Chord,
        true,
        false)