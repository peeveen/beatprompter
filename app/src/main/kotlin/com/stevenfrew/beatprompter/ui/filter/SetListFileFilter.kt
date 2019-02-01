package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.set.SetListEntry

open class SetListFileFilter(var mSetListFile: SetListFile,
                             songs: MutableList<SongFile>)
    : SetListFilter(mSetListFile.mSetTitle, getSongList(mSetListFile.mSetListEntries, songs)) {
    var mMissingSetListEntries = getMissingSetListEntries(mSetListFile.mSetListEntries, mSongs)
    var mWarned = mMissingSetListEntries.isEmpty()

    companion object {
        private fun getSongList(setListEntries: List<SetListEntry>, songFiles: List<SongFile>): MutableList<SongFile> {
            val matches = getMatches(setListEntries, songFiles)
            val matchedSongs = matches.map {
                when {
                    it.second != null -> it.second
                    it.third != null -> it.third
                    else -> null
                }
            }
            return matchedSongs.filterNotNull().toMutableList()
        }

        private fun getMissingSetListEntries(setListEntries: List<SetListEntry>, songFiles: List<SongFile>): MutableList<SetListEntry> {
            val matches = getMatches(setListEntries, songFiles)
            val invalidEntries = matches.map {
                if (it.second == null && it.third == null)
                    it.first
                else null
            }
            return invalidEntries.filterNotNull().toMutableList()
        }

        private fun getMatches(setListEntries: List<SetListEntry>,
                               songFiles: List<SongFile>): List<Triple<SetListEntry, SongFile?, SongFile?>> {
            return setListEntries.map { entry ->
                val exactMatch = songFiles.firstOrNull { song: SongFile -> entry.matches(song) == SetListMatch.TitleAndArtistMatch }
                val inexactMatch = songFiles.firstOrNull { song: SongFile -> entry.matches(song) == SetListMatch.TitleMatch }
                Triple(entry, exactMatch, inexactMatch)
            }
        }
    }
}
