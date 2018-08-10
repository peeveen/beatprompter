package com.stevenfrew.beatprompter.filter

import com.stevenfrew.beatprompter.cache.SongFile
import java.util.ArrayList

class FolderFilter(folderName: String, songs: ArrayList<SongFile>) : SongFilter(folderName, songs, true) {

    override fun equals(other: Any?): Boolean {
        return other is FolderFilter && mName == other.mName
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}