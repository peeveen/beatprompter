package com.stevenfrew.beatprompter.filter;

import com.stevenfrew.beatprompter.cache.SongFile;

import java.util.List;

public class SongFilter extends Filter
{
    public List<SongFile> mSongs;
    SongFilter(String name, List<SongFile> songs, boolean canSort)
    {
        super(name,canSort);
        mSongs=songs;
    }
}
