package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.cache.SongFile

data class SongLoadInfo(var songFile: SongFile, var track: String, var scrollMode: ScrollingMode, var nextSong: String, var startedByBandLeader: Boolean, var startedByMIDITrigger: Boolean, var nativeDisplaySettings: SongDisplaySettings, var sourceDisplaySettings: SongDisplaySettings)