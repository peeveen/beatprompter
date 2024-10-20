package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.cache.SongFile
import kotlin.random.Random

internal class Playlist private constructor(
	val nodes: Array<PlaylistNode>
) {
	private val songFiles: List<Pair<SongFile, String?>>
		get() = nodes.map { it.songFile to it.variation }

	constructor() : this(buildSongList(listOf()))
	constructor(songs: List<Pair<SongFile, String?>>) : this(buildSongList(songs))

	fun sortByTitle(): Playlist =
		Playlist(buildSongList(songFiles.sortedBy { it.first.sortableTitle }))

	fun sortByMode(): Playlist =
		Playlist(buildSongList(songFiles.sortedBy { it.first.bestScrollingMode }))

	fun sortByRating(): Playlist =
		Playlist(buildSongList(songFiles.sortedByDescending { it.first.rating }))  // Sort from best to worst

	fun sortByArtist(): Playlist =
		Playlist(buildSongList(songFiles.sortedBy { it.first.sortableArtist }))

	fun sortByKey(): Playlist = Playlist(buildSongList(songFiles.sortedBy { it.first.key }))
	fun sortByDateModified(): Playlist =
		Playlist(buildSongList(songFiles.sortedByDescending { it.first.lastModified }))

	fun shuffle(): Playlist =
		Playlist(buildSongList(songFiles.map { it to Random.Default.nextDouble() }
			.sortedBy { it.second }.map { it.first }))

	companion object {
		private fun buildSongList(songs: List<Pair<SongFile, String?>>): Array<PlaylistNode> {
			var lastNode: PlaylistNode? = null
			return songs.reversed().map {
				val node = PlaylistNode(it.first, it.second, lastNode)
				lastNode = node
				node
			}.reversed().toTypedArray()
		}
	}
}
