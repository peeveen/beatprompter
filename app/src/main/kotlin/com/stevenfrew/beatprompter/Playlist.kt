package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.cache.SongFile

internal class Playlist {
    private val mItems = mutableListOf<PlaylistNode>()

    private val songFiles: List<SongFile>
        get() {
            return mItems.map{it.mSongFile}
        }

    val nodesAsArray: List<PlaylistNode>
        get() = mItems

    constructor() {
        buildSongList(ArrayList())
    }

    constructor(songs: List<SongFile>) {
        buildSongList(songs)
    }

    fun getNodeAt(position: Int): PlaylistNode {
        return mItems[position]
    }

    fun sortByTitle() {
        buildSongList(songFiles.sortedBy{it.mSortableTitle})
    }

    fun sortByArtist() {
        buildSongList(songFiles.sortedBy{it.mSortableArtist})
    }

    fun sortByKey() {
        buildSongList( songFiles.sortedBy{it.mKey})
    }

    fun sortByDateModified() {
        buildSongList(songFiles.sortedBy{it.mLastModified})
    }

    private fun buildSongList(songs: List<SongFile>) {
        mItems.clear()
        for (sf in songs) {
            val node=PlaylistNode(sf)
            mItems.lastOrNull()?.mNextNode=node
            mItems.add(node)
        }
    }
}
