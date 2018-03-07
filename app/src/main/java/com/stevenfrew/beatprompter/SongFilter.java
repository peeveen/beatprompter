package com.stevenfrew.beatprompter;

import java.util.ArrayList;

class SongFilter extends Filter
{
    ArrayList<SongFile> mSongs;
    SongFilter(String name, ArrayList<SongFile> songs, boolean canSort)
    {
        super(name,canSort);
        mSongs=songs;
    }
}
