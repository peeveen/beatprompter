package com.stevenfrew.beatprompter.song.load

import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.song.ScrollingMode
import java.util.UUID

data class SongLoadInfo(
	val songFile: SongFile,
	val variation: String,
	val songLoadMode: ScrollingMode,
	val nextSong: String,
	val wasStartedByBandLeader: Boolean,
	val wasStartedByMidiTrigger: Boolean,
	val nativeDisplaySettings: DisplaySettings,
	val sourceDisplaySettings: DisplaySettings,
	val noAudio: Boolean,
	val audioLatency: Int
) {
	val loadId = UUID.randomUUID()!!
	val initialScrollMode
		get() = if (mixedModeActive) ScrollingMode.Manual else songLoadMode
	val mixedModeActive
		get() = songFile.isMixedMode && songLoadMode == ScrollingMode.Beat
}