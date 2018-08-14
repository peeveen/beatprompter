package com.stevenfrew.beatprompter.filter

import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.SongFile
import java.util.ArrayList
import java.util.HashMap

open class SetListFileFilter(var mSetListFile: SetListFile, songs: MutableList<SongFile>) : SetListFilter(mSetListFile.mSetTitle, getSongList(mSetListFile.mSongTitles, songs)) {
    var mMissingSongs: MutableList<String>
    var mWarned: Boolean = false

    init {
        mMissingSongs = getMissingSongList(mSetListFile.mSongTitles, mSongs)
        mWarned = mMissingSongs.size == 0
    }

    companion object {
        private fun getSongList(titles: List<String>, songFiles: List<SongFile>): MutableList<SongFile> {
            val songsByTitle = HashMap<String, SongFile>()
            for (sf in songFiles)
                songsByTitle[normalizeTitle(sf.mTitle)] = sf
            val foundSongs = ArrayList<SongFile>()
            for (title in titles) {
                val sf = songsByTitle[normalizeTitle(title)]
                if (sf != null)
                    foundSongs.add(sf)
            }
            return foundSongs
        }

        private fun getMissingSongList(titles: List<String>, songFiles: List<SongFile>): MutableList<String> {
            val songsByTitle = HashMap<String, SongFile>()
            for (sf in songFiles)
                songsByTitle[normalizeTitle(sf.mTitle)] = sf
            val missingSongs = ArrayList<String>()
            for (title in titles) {
                val sf = songsByTitle[normalizeTitle(title)]
                if (sf == null)
                    missingSongs.add(title)
            }
            return missingSongs
        }

        private fun normalizeTitle(title: String): String {
            var normalized = title.replace('’', '\'')
            normalized = normalized.replace("\uFEFF", "")
            return normalized.toLowerCase()
        }
    }
}
