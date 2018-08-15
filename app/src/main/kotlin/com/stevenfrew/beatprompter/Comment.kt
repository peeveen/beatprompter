package com.stevenfrew.beatprompter

class Comment internal constructor(var mText: String, audience: String) {
    private val commentAudience = audience.toLowerCase().split("@".toRegex()).dropWhile { it.trim().isEmpty() }.toList()

    fun isIntendedFor(audience: String?): Boolean {
        return (commentAudience.isEmpty()) ||
                (audience == null) ||
                (audience.isBlank()) ||
                audience.toLowerCase().split(",".toRegex()).dropWhile { it.trim().isEmpty() }.intersect(commentAudience).any()
    }
}