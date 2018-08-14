package com.stevenfrew.beatprompter.filter

import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.SongFile

open class SetListFileFilter(var mSetListFile: SetListFile, songs: MutableList<SongFile>) : SetListFilter(mSetListFile.mSetTitle, getSongList(mSetListFile.mSongTitles, songs)) {
    var mMissingSongs=getMissingSongList(mSetListFile.mSongTitles, mSongs)
    var mWarned= mMissingSongs.size == 0

    companion object {
        private fun getSongList(titles: List<String>, songFiles: List<SongFile>): MutableList<SongFile> {
            return songFiles.filter{ song -> titles.map{normalizeTitle(it)}.contains(normalizeTitle(song.mTitle))}.toMutableList()
        }

        private fun getMissingSongList(titles: List<String>, songFiles: List<SongFile>): MutableList<String> {
            return songFiles.filter{ song ->!titles.map{normalizeTitle(it)}.contains(normalizeTitle(song.mTitle))}.map{it.mTitle}.toMutableList()
        }

        private fun normalizeTitle(title: String): String {
            return title.replace('’', '\'').replace("\uFEFF", "").toLowerCase()
        }
    }
}
