package com.stevenfrew.beatprompter.filter;

import com.stevenfrew.beatprompter.cache.SongFile;

import java.util.List;

public class AllSongsFilter extends SongFilter
{
    public AllSongsFilter(String name,List<SongFile> songs)
    {
        super(name,songs,true);
    }
    public boolean equals(Object o) {
        return o == null || o instanceof AllSongsFilter;
    }
}
