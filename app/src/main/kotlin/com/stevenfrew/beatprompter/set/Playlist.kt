package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.cache.SongFile
import kotlin.random.Random

internal class Playlist private constructor(
	val nodes: Array<PlaylistNode>
) {
	private val songFiles: List<SongFile>
		get() = nodes.map { it.songFile }

	constructor() : this(buildSongList(listOf()))
	constructor(songs: List<SongFile>) : this(buildSongList(songs))

	fun sortByTitle(): Playlist = Playlist(buildSongList(songFiles.sortedBy { it.sortableTitle }))
	fun sortByMode(): Playlist = Playlist(buildSongList(songFiles.sortedBy { it.bestScrollingMode }))
	fun sortByRating(): Playlist =
		Playlist(buildSongList(songFiles.sortedByDescending { it.rating }))  // Sort from best to worst

	fun sortByArtist(): Playlist = Playlist(buildSongList(songFiles.sortedBy { it.sortableArtist }))
	fun sortByKey(): Playlist = Playlist(buildSongList(songFiles.sortedBy { it.key }))
	fun sortByDateModified(): Playlist =
		Playlist(buildSongList(songFiles.sortedByDescending { it.lastModified }))

	fun shuffle(): Playlist =
		Playlist(buildSongList(songFiles.map { it to Random.Default.nextDouble() }
			.sortedBy { it.second }.map { it.first }))

	companion object {
		private fun buildSongList(songs: List<SongFile>): Array<PlaylistNode> {
			var lastNode: PlaylistNode? = null
			return songs.reversed().map {
				val node = PlaylistNode(it, lastNode)
				lastNode = node
				node
			}.reversed().toTypedArray()
		}
	}
}
