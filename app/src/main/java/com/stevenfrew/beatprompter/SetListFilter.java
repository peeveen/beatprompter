package com.stevenfrew.beatprompter;

import java.util.ArrayList;

class SetListFilter extends SongFilter
{
    SetListFilter(String name,ArrayList<SongFile> songs)
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
