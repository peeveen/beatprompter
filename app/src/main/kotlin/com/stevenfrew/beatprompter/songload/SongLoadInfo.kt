package com.stevenfrew.beatprompter.songload

import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.ScrollingMode
import com.stevenfrew.beatprompter.SongDisplaySettings
import com.stevenfrew.beatprompter.cache.AudioFile

data class SongLoadInfo(var songFile: SongFile, var track: AudioFile?, var scrollMode: ScrollingMode, var nextSong: String, var startedByBandLeader: Boolean, var startedByMIDITrigger: Boolean, var nativeDisplaySettings: SongDisplaySettings, var sourceDisplaySettings: SongDisplaySettings)