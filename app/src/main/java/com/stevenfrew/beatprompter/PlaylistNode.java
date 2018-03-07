package com.stevenfrew.beatprompter;

class PlaylistNode
{
    SongFile mSongFile;
    PlaylistNode mNextNode;
    PlaylistNode mPrevNode;

    PlaylistNode(SongFile song)
    {
        mSongFile=song;
    }
}
