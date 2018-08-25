package com.stevenfrew.beatprompter

enum class LineScrollingMode {
    Beat,Smooth,Manual;

    fun toSongScrollingMode():SongScrollingMode
    {
        return when(this)
        {
            Beat->SongScrollingMode.Beat
            Smooth->SongScrollingMode.Smooth
            Manual->SongScrollingMode.Manual
        }
    }
}