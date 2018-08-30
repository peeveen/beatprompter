package com.stevenfrew.beatprompter.songload

import com.stevenfrew.beatprompter.SongScrollingMode

enum class SongLoadMode {
    Beat,Smooth,Manual;

    fun toSongScrollMode(): SongScrollingMode
    {
        return when(this)
        {
            Beat->SongScrollingMode.Beat
            Smooth->SongScrollingMode.Smooth
            else->SongScrollingMode.Manual
        }
    }
}