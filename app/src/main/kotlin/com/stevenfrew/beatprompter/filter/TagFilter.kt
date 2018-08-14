package com.stevenfrew.beatprompter.filter

import com.stevenfrew.beatprompter.cache.SongFile

class TagFilter(tag: String, songs: MutableList<SongFile>) : SongFilter(tag, songs, true) {

    override fun equals(other: Any?): Boolean {
        return other is TagFilter && mName == other.mName
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}