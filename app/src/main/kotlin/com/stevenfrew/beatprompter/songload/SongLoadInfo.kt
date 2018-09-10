package com.stevenfrew.beatprompter.songload

import com.stevenfrew.beatprompter.ScrollingMode
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.graphics.SongDisplaySettings
import com.stevenfrew.beatprompter.cache.AudioFile

data class SongLoadInfo(var mSongFile: SongFile, var mTrack: AudioFile?, var mSongLoadMode: ScrollingMode, var mNextSong: String, var mStartedByBandLeader: Boolean, var mStartedByMIDITrigger: Boolean, var mNativeDisplaySettings: SongDisplaySettings, var mSourceDisplaySettings: SongDisplaySettings)
{
    val initialScrollMode
        get()= if(mixedModeActive) ScrollingMode.Manual else mSongLoadMode
    val mixedModeActive
        get()= mSongFile.mMixedMode && mSongLoadMode==ScrollingMode.Beat
}