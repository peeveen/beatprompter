package com.stevenfrew.beatprompter.filter

import com.stevenfrew.beatprompter.cache.SongFile
import java.util.ArrayList

class TagFilter(tag: String, songs: ArrayList<SongFile>) : SongFilter(tag, songs, true) {

    override fun equals(other: Any?): Boolean {
        return other is TagFilter && mName == other.mName
    }
}