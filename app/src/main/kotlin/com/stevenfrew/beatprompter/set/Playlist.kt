package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.song.SongInfo
import com.stevenfrew.beatprompter.util.bestScrollingMode
import kotlin.random.Random

internal class Playlist private constructor(
	val nodes: Array<PlaylistNode>
) {
	private val songs: List<Pair<SongInfo, String?>>
		get() = nodes.map { it.songInfo to it.variation }

	constructor() : this(buildSongList(listOf()))
	constructor(songs: List<Pair<SongInfo, String?>>) : this(buildSongList(songs))

	fun sortByTitle(): Playlist =
		Playlist(buildSongList(songs.sortedBy { it.first.sortableTitle }))

	fun sortByMode(): Playlist =
		Playlist(buildSongList(songs.sortedBy { it.first.bestScrollingMode }))

	fun sortByRating(): Playlist =
		Playlist(buildSongList(songs.sortedByDescending { it.first.rating }))  // Sort from best to worst

	fun sortByArtist(): Playlist =
		Playlist(buildSongList(songs.sortedBy { it.first.sortableArtist }))

	fun sortByKey(): Playlist = Playlist(buildSongList(songs.sortedBy { it.first.keySignature }))
	fun sortByYear(): Playlist = Playlist(buildSongList(songs.sortedBy { it.first.year }))
	fun sortByIcon(): Playlist = Playlist(buildSongList(songs.sortedBy { it.first.icon }))

	fun sortByDateModified(): Playlist =
		Playlist(buildSongList(songs.sortedByDescending { it.first.lastModified }))

	fun shuffle(): Playlist =
		Playlist(buildSongList(songs.map { it to Random.Default.nextDouble() }
			.sortedBy { it.second }.map { it.first }))

	companion object {
		private fun buildSongList(songs: List<Pair<SongInfo, String?>>): Array<PlaylistNode> {
			var lastNode: PlaylistNode? = null
			return songs.reversed().map {
				val node = PlaylistNode(it.first, it.second, lastNode)
				lastNode = node
				node
			}.reversed().toTypedArray()
		}
	}
}
