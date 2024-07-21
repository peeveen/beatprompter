package com.stevenfrew.beatprompter.song.load

import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.song.ScrollingMode
import java.util.UUID

data class SongLoadInfo(
	val mSongFile: SongFile,
	val mVariation: String,
	val mSongLoadMode: ScrollingMode,
	val mNextSong: String,
	val mStartedByBandLeader: Boolean,
	val mStartedByMIDITrigger: Boolean,
	val mNativeDisplaySettings: DisplaySettings,
	val mSourceDisplaySettings: DisplaySettings,
	val mNoAudio: Boolean,
	val mAudioLatency: Int
) {
	val mLoadID = UUID.randomUUID()!!
	val initialScrollMode
		get() = if (mixedModeActive) ScrollingMode.Manual else mSongLoadMode
	val mixedModeActive
		get() = mSongFile.mMixedMode && mSongLoadMode == ScrollingMode.Beat
}