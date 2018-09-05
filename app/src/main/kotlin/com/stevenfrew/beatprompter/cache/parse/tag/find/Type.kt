package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Supported tag types.
 */
enum class Type {
    // {Directive} tags
    Directive,

    // [Chord] tags
    Chord,

    // ,,Shorthand<< tags
    Shorthand
}