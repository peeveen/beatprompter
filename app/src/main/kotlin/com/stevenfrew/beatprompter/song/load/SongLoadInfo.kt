package com.stevenfrew.beatprompter.song.load

import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.song.ScrollingMode
import java.util.UUID

data class SongLoadInfo(
	val songFile: SongFile,
	val variation: String,
	val songLoadMode: ScrollingMode,
	val nativeDisplaySettings: DisplaySettings,
	val sourceDisplaySettings: DisplaySettings,
	val nextSong: String = "",
	val wasStartedByBandLeader: Boolean = false,
	val wasStartedByMidiTrigger: Boolean = false,
	val noAudio: Boolean = false,
	val audioLatency: Int = 0,
	val transposeShift: Int = 0
) {
	val loadId = UUID.randomUUID()!!
	val initialScrollMode
		get() = if (mixedModeActive) ScrollingMode.Manual else songLoadMode
	val mixedModeActive
		get() = songFile.mixedModeVariations.contains(variation) && songLoadMode == ScrollingMode.Beat
}