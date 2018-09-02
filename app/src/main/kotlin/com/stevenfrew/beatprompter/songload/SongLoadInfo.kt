package com.stevenfrew.beatprompter.songload

import com.stevenfrew.beatprompter.ScrollingMode
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.SongDisplaySettings
import com.stevenfrew.beatprompter.cache.AudioFile

data class SongLoadInfo(var mSongFile: SongFile, var mTrack: AudioFile?, var mSongLoadMode: ScrollingMode, var mNextSong: String, var mStartedByBandLeader: Boolean, var mStartedByMIDITrigger: Boolean, var mNativeDisplaySettings: SongDisplaySettings, var mSourceDisplaySettings: SongDisplaySettings)
{
    val initialScrollMode
        get()= if(mSongFile.mMixedMode) ScrollingMode.Manual else mSongLoadMode
}