package com.stevenfrew.beatprompter.filter;

import com.stevenfrew.beatprompter.cache.SongFile;

import java.util.ArrayList;

public class TagFilter extends SongFilter
{
    public TagFilter(String tag,ArrayList<SongFile> songs)
    {
        super(tag,songs,true);
    }

    public boolean equals(Object o) {
        return o instanceof TagFilter && mName.equals(((TagFilter) o).mName);
    }
}
