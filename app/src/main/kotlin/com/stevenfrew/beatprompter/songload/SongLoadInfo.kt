package com.stevenfrew.beatprompter.songload

import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.SongDisplaySettings
import com.stevenfrew.beatprompter.cache.AudioFile

data class SongLoadInfo(var mSongFile: SongFile, var mTrack: AudioFile?, var mSongLoadMode: SongLoadMode, var mNextSong: String, var mStartedByBandLeader: Boolean, var mStartedByMIDITrigger: Boolean, var mNativeDisplaySettings: SongDisplaySettings, var mSourceDisplaySettings: SongDisplaySettings)