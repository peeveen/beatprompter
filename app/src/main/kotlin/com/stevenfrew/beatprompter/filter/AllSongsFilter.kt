package com.stevenfrew.beatprompter.filter

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.SongFile

class AllSongsFilter(songs: MutableList<SongFile>) : SongFilter(BeatPrompterApplication.getResourceString(R.string.no_tag_selected), songs, true) {
    override fun equals(other: Any?): Boolean {
        return other == null || other is AllSongsFilter
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}