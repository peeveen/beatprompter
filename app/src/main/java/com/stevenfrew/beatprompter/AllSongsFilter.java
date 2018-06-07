package com.stevenfrew.beatprompter;

import java.util.List;

class AllSongsFilter extends SongFilter
{
    AllSongsFilter(String name,List<SongFile> songs)
    {
        super(name,songs,true);
    }
    public boolean equals(Object o) {
        return o == null || o instanceof AllSongsFilter;
    }
}
