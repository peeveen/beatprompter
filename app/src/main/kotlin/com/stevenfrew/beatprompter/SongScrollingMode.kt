package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.songload.SongLoadMode

enum class SongScrollingMode {
    Beat, Smooth, Manual, Mixed;

    fun toLineScrollMode():LineScrollingMode
    {
        return when(this)
        {
            SongLoadMode.Beat ->LineScrollingMode.Beat
            SongLoadMode.Smooth ->LineScrollingMode.Smooth
            else->LineScrollingMode.Manual
        }
    }
}