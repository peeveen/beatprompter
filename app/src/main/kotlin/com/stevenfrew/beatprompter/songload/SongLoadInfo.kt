package com.stevenfrew.beatprompter.songload

import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.ScrollingMode
import com.stevenfrew.beatprompter.SongDisplaySettings

data class SongLoadInfo(var songFile: SongFile, var track: String, var scrollMode: ScrollingMode, var nextSong: String, var startedByBandLeader: Boolean, var startedByMIDITrigger: Boolean, var nativeDisplaySettings: SongDisplaySettings, var sourceDisplaySettings: SongDisplaySettings)