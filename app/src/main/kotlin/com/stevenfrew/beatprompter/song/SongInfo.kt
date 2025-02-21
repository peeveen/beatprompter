package com.stevenfrew.beatprompter.song

interface SongInfo {
	val title: String
	val artist: String
	val icon: String?
	val isBeatScrollable: Boolean
	val isSmoothScrollable: Boolean
	val rating: Int
	val year: Int?
	val votes: Int
	val keySignature: String?
	val audioFiles: Map<String, List<String>>
}