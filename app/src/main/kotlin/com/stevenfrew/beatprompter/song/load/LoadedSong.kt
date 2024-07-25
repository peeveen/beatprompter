package com.stevenfrew.beatprompter.song.load

import com.stevenfrew.beatprompter.song.Song

data class LoadedSong(
	val song: Song,
	val loadJob: SongLoadJob
)