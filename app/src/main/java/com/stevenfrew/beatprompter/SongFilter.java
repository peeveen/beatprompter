package com.stevenfrew.beatprompter;

import java.util.List;

class SongFilter extends Filter
{
    List<SongFile> mSongs;
    SongFilter(String name, List<SongFile> songs, boolean canSort)
    {
        super(name,canSort);
        mSongs=songs;
    }
}
