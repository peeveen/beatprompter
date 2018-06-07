package com.stevenfrew.beatprompter;

import com.stevenfrew.beatprompter.cache.SongFile;

public class PlaylistNode
{
    public SongFile mSongFile;
    PlaylistNode mNextNode;
    PlaylistNode mPrevNode;

    PlaylistNode(SongFile song)
    {
        mSongFile=song;
    }
}
