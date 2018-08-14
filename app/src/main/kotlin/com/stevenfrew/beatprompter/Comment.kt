package com.stevenfrew.beatprompter

class Comment internal constructor(var mText: String, audience: String) {
    private val commentAudience = audience.toLowerCase().split("@".toRegex()).dropWhile { it.trim().isEmpty() }.toList()

    fun isIntendedFor(audience: String?): Boolean {
        if (commentAudience.isEmpty())
            return true
        if (audience == null || audience.isEmpty())
            return true
        return audience.toLowerCase().split(",".toRegex()).dropWhile { it.trim().isEmpty() }.intersect(commentAudience).any()
    }
}