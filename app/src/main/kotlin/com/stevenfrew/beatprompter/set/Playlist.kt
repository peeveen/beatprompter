package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.cache.SongFile
import kotlin.random.Random

internal class Playlist {
	private val mItems = mutableListOf<PlaylistNode>()

	private val songFiles: List<SongFile>
		get() {
			return mItems.map { it.mSongFile }
		}

	val nodes: List<PlaylistNode>
		get() = mItems

	constructor() {
		buildSongList(ArrayList())
	}

	constructor(songs: List<SongFile>) {
		buildSongList(songs)
	}

	fun sortByTitle() {
		buildSongList(songFiles.sortedBy { it.mSortableTitle })
	}

	fun sortByMode() {
		buildSongList(songFiles.sortedBy { it.bestScrollingMode })
	}

	fun sortByArtist() {
		buildSongList(songFiles.sortedBy { it.mSortableArtist })
	}

	fun sortByKey() {
		buildSongList(songFiles.sortedBy { it.mKey })
	}

	fun sortByDateModified() {
		buildSongList(songFiles.sortedByDescending { it.mLastModified })
	}

	fun shuffle() {
		val randomizedSongs = songFiles.map { it to Random.Default.nextDouble() }
		buildSongList(randomizedSongs.sortedBy { it.second }.map { it.first })
	}

	private fun buildSongList(songs: List<SongFile>) {
		mItems.clear()
		songs.forEach {
			val node = PlaylistNode(it)
			mItems.lastOrNull()?.mNextNode = node
			mItems.add(node)
		}
	}
}
