package com.stevenfrew.beatprompter.filter;

import com.stevenfrew.beatprompter.cache.SongFile;

import java.util.ArrayList;

public class TemporarySetListFilter extends SetListFilter
{
    public TemporarySetListFilter(String name,ArrayList<SongFile> songs)
    {
        super(name,songs);
    }
    public void addSong(SongFile sf)
    {
        mSongs.add(sf);
    }
    public boolean equals(Object o) {
        return o!=null && o instanceof TemporarySetListFilter;
    }
}
