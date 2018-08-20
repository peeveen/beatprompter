package com.stevenfrew.beatprompter.filter

import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.cache.parse.SetListEntry

open class SetListFileFilter(var mSetListFile: SetListFile, songs: MutableList<SongFile>) : SetListFilter(mSetListFile.mSetTitle, getSongList(mSetListFile.mSetListEntries, songs)) {
    var mMissingSongs=getMissingSongList(mSetListFile.mSetListEntries, mSongs)
    var mWarned= mMissingSongs.size == 0

    companion object {
        private fun getSongList(setListEntries: List<SetListEntry>, songFiles: List<SongFile>): MutableList<SongFile> {
            return songFiles.filter{ song -> setListEntries.map{normalizeTitle(it)}.contains(normalizeTitle(song.mTitle))}.toMutableList()
        }

        private fun getMissingSongList(setListEntries: List<SetListEntry>, songFiles: List<SongFile>): MutableList<String> {
            return songFiles.filter{ song ->!setListEntries.map{normalizeTitle(it)}.contains(normalizeTitle(song.mTitle))}.map{it.mTitle}.toMutableList()
        }

        private fun normalizeTitle(setListEntry: String): String {
            return setListEntry.replace('’', '\'').replace("\uFEFF", "").toLowerCase()
        }
    }
}
