package com.stevenfrew.beatprompter.filter;

import com.stevenfrew.beatprompter.cache.SetListFile;
import com.stevenfrew.beatprompter.cache.SongFile;

import java.util.List;

public class TemporarySetListFilter extends SetListFileFilter
{
    public TemporarySetListFilter(SetListFile setListFile, List<SongFile> songs)
    {
        super(setListFile,songs);
    }
    public void addSong(SongFile sf)
    {
        mSongs.add(sf);
    }
    public boolean equals(Object o) {
        return o!=null && o instanceof TemporarySetListFilter;
    }
    public void clear()
    {
        mMissingSongs.clear();
        mSongs.clear();
    }
}
