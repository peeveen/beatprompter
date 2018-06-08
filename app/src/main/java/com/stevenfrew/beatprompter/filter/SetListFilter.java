package com.stevenfrew.beatprompter.filter;

import com.stevenfrew.beatprompter.cache.SongFile;

import java.util.List;

public class SetListFilter extends SongFilter
{
    SetListFilter(String name,List<SongFile> songs)
    {
        super(name,songs,false);
    }

    public boolean containsSong(SongFile sf)
    {
        return mSongs.contains(sf);
    }

    public boolean equals(Object o) {
        return o instanceof SetListFilter && mName.equals(((SetListFilter) o).mName);
    }
}
