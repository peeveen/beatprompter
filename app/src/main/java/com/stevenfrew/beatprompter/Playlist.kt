package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.cache.SongFile
import java.util.ArrayList

internal class Playlist {
    private val mItems = ArrayList<PlaylistNode>()

    private val songFiles: List<SongFile>
        get() {
            val songs = ArrayList<SongFile>()
            for (node in mItems)
                songs.add(node.mSongFile)
            return songs
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
        buildSongList(songFiles.sortedBy{it.sortableTitle})
    }

    fun sortByArtist() {
        buildSongList(songFiles.sortedBy{it.sortableArtist})
    }

    fun sortByKey() {
        buildSongList( songFiles.sortedBy{it.mKey})
    }

    fun sortByDateModified() {
        buildSongList(songFiles.sortedBy{it.mLastModified})
    }

    private fun buildSongList(songs: List<SongFile>) {
        mItems.clear()
        var lastNode: PlaylistNode? = null
        for (sf in songs) {
            val node = PlaylistNode(sf)
            node.mPrevNode = lastNode
            if (lastNode != null)
                lastNode.mNextNode = node
            mItems.add(node)
            lastNode = node
        }
    }
}
