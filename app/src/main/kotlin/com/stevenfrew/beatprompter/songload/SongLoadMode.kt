package com.stevenfrew.beatprompter.songload

import com.stevenfrew.beatprompter.ScrollingMode

enum class SongLoadMode {
    Beat,Smooth,Manual;

    fun toLineScrollMode(): ScrollingMode
    {
        return when(this)
        {
            Beat->ScrollingMode.Beat
            Smooth->ScrollingMode.Smooth
            else->ScrollingMode.Manual
        }
    }
}