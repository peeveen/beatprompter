package com.stevenfrew.beatprompter;


import com.stevenfrew.beatprompter.cache.SongFile;

class SongLoadInfo {
    SongFile mSongFile;
    String mTrack;
    ScrollingMode mScrollMode;
    SongDisplaySettings mNativeDisplaySettings;
    boolean mStartedByBandLeader;
    boolean mStartedByMIDITrigger;
    String mNextSong;
    SongDisplaySettings mSourceDisplaySettings;

    SongLoadInfo(SongFile songFile, String track, ScrollingMode mode,String nextSong,boolean startedByBandLeader,boolean startedByMidiTrigger,SongDisplaySettings nativeSettings,SongDisplaySettings sourceSettings)
    {
        mSongFile=songFile;
        mStartedByMIDITrigger=startedByMidiTrigger;
        mTrack=track;
        mScrollMode=mode;
        mNextSong=nextSong;
        mStartedByBandLeader=startedByBandLeader;
        mNativeDisplaySettings=nativeSettings;
        mSourceDisplaySettings=sourceSettings;
    }
}

