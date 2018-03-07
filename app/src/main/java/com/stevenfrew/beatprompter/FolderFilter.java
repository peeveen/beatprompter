package com.stevenfrew.beatprompter;

import java.util.ArrayList;

class FolderFilter extends SongFilter
{
    FolderFilter(String folderName,ArrayList<SongFile> songs)
    {
        super(folderName,songs,true);
    }

    public boolean equals(Object o) {
        return o instanceof FolderFilter && mName.equals(((FolderFilter) o).mName);
    }
}
