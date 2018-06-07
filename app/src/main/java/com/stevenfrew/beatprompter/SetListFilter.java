package com.stevenfrew.beatprompter;

import java.util.List;

class SetListFilter extends SongFilter
{
    SetListFilter(String name,List<SongFile> songs)
    {
        super(name,songs,false);
    }

    boolean containsSong(SongFile sf)
    {
        return mSongs.contains(sf);
    }

    public boolean equals(Object o) {
        return o instanceof SetListFilter && mName.equals(((SetListFilter) o).mName);
    }
}
