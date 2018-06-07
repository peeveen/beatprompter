package com.stevenfrew.beatprompter;

import android.content.Context;
import android.os.Handler;

import java.io.IOException;
import java.util.ArrayList;

class LoadingSongFile {
    SongFile mSongFile;
    String mTrack;
    ScrollingMode mScrollMode;
    private boolean mStartedByBandLeader=false;
    private boolean mStartedByMIDITrigger=false;
    private String mNextSong;
    SongDisplaySettings mNativeDisplaySettings;
    SongDisplaySettings mSourceDisplaySettings;
    boolean mIsDemoSong;

    LoadingSongFile(SongFile songFile, String track, ScrollingMode mode,String nextSong,boolean startedByBandLeader,boolean startedByMidiTrigger,boolean isDemoSong,SongDisplaySettings nativeSettings,SongDisplaySettings sourceSettings)
    {
        mSongFile=songFile;
        mStartedByMIDITrigger=startedByMidiTrigger;
        mTrack=track;
        mIsDemoSong=isDemoSong;
        mScrollMode=mode;
        mNextSong=nextSong;
        mStartedByBandLeader=startedByBandLeader;
        mNativeDisplaySettings=nativeSettings;
        mSourceDisplaySettings=sourceSettings;
    }
    Song load(boolean fullVersionUnlocked, CancelEvent cancelEvent, Handler handler,ArrayList<MIDIAlias> midiAliases) throws IOException
    {
        return mSongFile.load(mScrollMode, mTrack,mIsDemoSong || fullVersionUnlocked,mStartedByBandLeader,mNextSong,cancelEvent,handler,mStartedByMIDITrigger,midiAliases,mNativeDisplaySettings,mSourceDisplaySettings);
    }
}