package com.stevenfrew.beatprompter;

import java.util.ArrayList;

class TemporarySetListFilter extends SetListFilter
{
    TemporarySetListFilter(String name,ArrayList<SongFile> songs)
    {
        super(name,songs);
    }
    void addSong(SongFile sf)
    {
        mSongs.add(sf);
    }
    public boolean equals(Object o) {
        return o!=null && o instanceof TemporarySetListFilter;
    }
}
