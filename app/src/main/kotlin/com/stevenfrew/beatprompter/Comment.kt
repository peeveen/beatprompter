package com.stevenfrew.beatprompter

import java.util.*

class Comment internal constructor(var mText: String, audience: String?) {
    private val commentAudience = ArrayList<String>()

    init {
        if (audience != null) {
            val bits = audience.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            Collections.addAll(commentAudience, *bits)
        }
    }

    fun isIntendedFor(audience: String?): Boolean {
        if (commentAudience.isEmpty())
            return true
        if (audience == null || audience.isEmpty())
            return true
        val bits = audience.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (bit in bits)
            for (a in commentAudience)
                if (bit.trim().equals(a.trim(), ignoreCase = true))
                    return true
        return false
    }
}