package com.stevenfrew.beatprompter.filter

import com.stevenfrew.beatprompter.cache.SongFile

open class SetListFilter internal constructor(name: String, songs: MutableList<SongFile>) : SongFilter(name, songs, false) {

    fun containsSong(sf: SongFile): Boolean {
        return mSongs.contains(sf)
    }

    override fun equals(other: Any?): Boolean {
        return other is SetListFilter && mName == other.mName
    }
}