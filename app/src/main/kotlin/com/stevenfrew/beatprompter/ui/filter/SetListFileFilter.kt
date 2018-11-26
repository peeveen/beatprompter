package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.set.SetListEntry

open class SetListFileFilter(var mSetListFile: SetListFile, songs: MutableList<SongFile>) : SetListFilter(mSetListFile.mSetTitle, getSongList(mSetListFile.mSetListEntries, songs)) {
    var mMissingSetListEntries = getMissingSetListEntries(mSetListFile.mSetListEntries, mSongs)
    var mWarned = mMissingSetListEntries.isEmpty()

    companion object {
        private fun getSongList(setListEntries: List<SetListEntry>, songFiles: List<SongFile>): MutableList<SongFile> {
            val copiedSongList = songFiles.toMutableList()
            val copiedSetListEntries = setListEntries.toMutableList()
            val fullMatches = getMatches(copiedSetListEntries, copiedSongList, SetListMatch.TitleAndArtistMatch)
            copiedSetListEntries.removeAll(fullMatches.map { it.first })
            val fullyMatchedSongs = fullMatches.map { it.second }
            copiedSongList.removeAll(fullyMatchedSongs)
            val partialMatches = getMatches(copiedSetListEntries, copiedSongList, SetListMatch.TitleMatch)
            val partiallyMatchedSongs = partialMatches.map { it.second }
            return listOf(fullyMatchedSongs, partiallyMatchedSongs).flatMap { it }.toMutableList()
        }

        private fun getMissingSetListEntries(setListEntries: List<SetListEntry>, songFiles: List<SongFile>): MutableList<SetListEntry> {
            val copiedSongList = songFiles.toMutableList()
            val copiedSetListEntries = setListEntries.toMutableList()
            val fullMatches = getMatches(copiedSetListEntries, copiedSongList, SetListMatch.TitleAndArtistMatch)
            copiedSetListEntries.removeAll(fullMatches.map { it.first })
            val fullyMatchedSongs = fullMatches.map { it.second }
            copiedSongList.removeAll(fullyMatchedSongs)
            val partialMatches = getMatches(copiedSetListEntries, copiedSongList, SetListMatch.TitleMatch)
            copiedSetListEntries.removeAll(partialMatches.map { it.first })
            val partiallyMatchedSongs = partialMatches.map { it.second }
            copiedSongList.removeAll(partiallyMatchedSongs)
            return copiedSetListEntries
        }

        private fun getMatches(setListEntries: List<SetListEntry>, songFiles: List<SongFile>, desiredMatchType: SetListMatch): List<Pair<SetListEntry, SongFile>> {
            return setListEntries.mapNotNull { entry ->
                val fullMatch = songFiles.firstOrNull { song: SongFile -> entry.matches(song) == desiredMatchType }
                if (fullMatch != null)
                    entry to fullMatch
                else
                    null
            }
        }
    }
}
