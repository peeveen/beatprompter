package com.stevenfrew.beatprompter

class Comment internal constructor(var mText: String, audience: List<String>) {
    private val commentAudience = audience

    fun isIntendedFor(audience: String?): Boolean {
        return (commentAudience.isEmpty()) ||
                (audience == null) ||
                (audience.isBlank()) ||
                audience.toLowerCase().split(",".toRegex()).dropWhile { it.trim().isEmpty() }.intersect(commentAudience).any()
    }
}