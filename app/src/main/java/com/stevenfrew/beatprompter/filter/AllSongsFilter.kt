package com.stevenfrew.beatprompter.filter

import com.stevenfrew.beatprompter.cache.SongFile

class AllSongsFilter(name: String, songs: MutableList<SongFile>) : SongFilter(name, songs, true) {
    override fun equals(other: Any?): Boolean {
        return other == null || other is AllSongsFilter
    }
}