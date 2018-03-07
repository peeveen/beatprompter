package com.stevenfrew.beatprompter;

import java.util.ArrayList;

class AllSongsFilter extends SongFilter
{
    AllSongsFilter(String name,ArrayList<SongFile> songs)
    {
        super(name,songs,true);
    }
    public boolean equals(Object o) {
        return o == null || o instanceof AllSongsFilter;
    }
}
