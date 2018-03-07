package com.stevenfrew.beatprompter;

import java.util.ArrayList;

class TagFilter extends SongFilter
{
    TagFilter(String tag,ArrayList<SongFile> songs)
    {
        super(tag,songs,true);
    }

    public boolean equals(Object o) {
        return o instanceof TagFilter && mName.equals(((TagFilter) o).mName);
    }
}
