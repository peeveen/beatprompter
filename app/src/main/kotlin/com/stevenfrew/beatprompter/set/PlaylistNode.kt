package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.cache.SongFile

data class PlaylistNode(val songFile: SongFile,val nextSong:PlaylistNode?=null)
