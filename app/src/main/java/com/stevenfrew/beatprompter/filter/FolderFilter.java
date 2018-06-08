package com.stevenfrew.beatprompter.filter;

import com.stevenfrew.beatprompter.cache.SongFile;

import java.util.ArrayList;

public class FolderFilter extends SongFilter
{
    public FolderFilter(String folderName,ArrayList<SongFile> songs)
    {
        super(folderName,songs,true);
    }

    public boolean equals(Object o) {
        return o instanceof FolderFilter && mName.equals(((FolderFilter) o).mName);
    }
}
