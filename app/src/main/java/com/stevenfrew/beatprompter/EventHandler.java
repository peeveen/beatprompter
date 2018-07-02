package com.stevenfrew.beatprompter;

import android.os.Handler;

import com.stevenfrew.beatprompter.pref.SettingsEventHandler;

public abstract class EventHandler extends Handler {
    private static final int HANDLER_MESSAGE_BASE_ID=1834739585;

    public static final int BLUETOOTH_MESSAGE_RECEIVED=HANDLER_MESSAGE_BASE_ID;
    public static final int CLIENT_CONNECTED=HANDLER_MESSAGE_BASE_ID+1;
    public static final int SERVER_CONNECTED=HANDLER_MESSAGE_BASE_ID+2;
    public static final int MIDI_START_SONG=HANDLER_MESSAGE_BASE_ID+3;
    public static final int MIDI_CONTINUE_SONG=HANDLER_MESSAGE_BASE_ID+4;
    public static final int MIDI_STOP_SONG=HANDLER_MESSAGE_BASE_ID+5;
    public static final int END_SONG=HANDLER_MESSAGE_BASE_ID+6;
    public static final int CLOUD_SYNC_ERROR=HANDLER_MESSAGE_BASE_ID+7;
    public static final int FOLDER_CONTENTS_FETCHING=HANDLER_MESSAGE_BASE_ID+8;
    public static final int MIDI_MSB_BANK_SELECT=HANDLER_MESSAGE_BASE_ID+9;
    public static final int MIDI_LSB_BANK_SELECT=HANDLER_MESSAGE_BASE_ID+10;
    public static final int MIDI_SONG_SELECT=HANDLER_MESSAGE_BASE_ID+11;
    public static final int MIDI_PROGRAM_CHANGE=HANDLER_MESSAGE_BASE_ID+12;
    public static final int SONG_LOAD_CANCELLED=HANDLER_MESSAGE_BASE_ID+13;
    public static final int SONG_LOAD_FAILED=HANDLER_MESSAGE_BASE_ID+14;
    public static final int SONG_LOAD_LINE_READ=HANDLER_MESSAGE_BASE_ID+15;
    public static final int SONG_LOAD_LINE_PROCESSED=HANDLER_MESSAGE_BASE_ID+16;
    public static final int SONG_LOAD_COMPLETED=HANDLER_MESSAGE_BASE_ID+17;
    public static final int MIDI_SET_SONG_POSITION=HANDLER_MESSAGE_BASE_ID+18;
    public static final int CACHE_UPDATED=HANDLER_MESSAGE_BASE_ID+19;
    public static final int SET_CLOUD_PATH=HANDLER_MESSAGE_BASE_ID+20;
    public static final int CLEAR_CACHE=HANDLER_MESSAGE_BASE_ID+21;
    public static final int FOLDER_CONTENTS_FETCHED=HANDLER_MESSAGE_BASE_ID+22;
    public static final int CLIENT_DISCONNECTED=HANDLER_MESSAGE_BASE_ID+23;
    public static final int SERVER_DISCONNECTED=HANDLER_MESSAGE_BASE_ID+24;

    private static final Object mSongListEventHandlerLock=new Object();
    private static final Object mSongDisplayEventHandlerLock=new Object();
    private static final Object mSettingsEventHandlerLock=new Object();
    private static SongListEventHandler mSongListEventHandler=null;
    private static SongDisplayEventHandler mSongDisplayEventHandler=null;
    private static Handler mSettingsEventHandler=null;

    public static void setSongListEventHandler(SongListEventHandler songListEventHandler)
    {
        synchronized(mSongListEventHandlerLock)
        {
            mSongListEventHandler = songListEventHandler;
        }
    }

    public static void setSongDisplayEventHandler(SongDisplayEventHandler songDisplayEventHandler)
    {
        synchronized(mSongDisplayEventHandlerLock) {
            mSongDisplayEventHandler = songDisplayEventHandler;
        }
    }

    public static void setSettingsEventHandler(SettingsEventHandler settingsEventHandler)
    {
        synchronized(mSettingsEventHandlerLock) {
            mSettingsEventHandler = settingsEventHandler;
        }
    }

    public static void sendEventToSongList(int event)
    {
        synchronized(mSongListEventHandlerLock) {
            if (mSongListEventHandler != null)
                mSongListEventHandler.obtainMessage(event).sendToTarget();
        }
    }

    public static void sendEventToSongDisplay(int event)
    {
        synchronized(mSongDisplayEventHandlerLock) {
            if (mSongDisplayEventHandler != null)
                mSongDisplayEventHandler.obtainMessage(event).sendToTarget();
        }
    }

    public static void sendEventToSettings(int event)
    {
        synchronized(mSettingsEventHandlerLock) {
            if (mSettingsEventHandler != null)
                mSettingsEventHandler.obtainMessage(event).sendToTarget();
        }
    }

    public static void sendEventToSongList(int event,Object arg)
    {
        synchronized(mSongListEventHandlerLock) {
            if (mSongListEventHandler != null)
                mSongListEventHandler.obtainMessage(event, arg).sendToTarget();
        }
    }

    public static void sendEventToSongDisplay(int event,Object arg)
    {
        synchronized(mSongDisplayEventHandlerLock) {
            if (mSongDisplayEventHandler != null)
                mSongDisplayEventHandler.obtainMessage(event, arg).sendToTarget();
        }
    }

    public static void sendEventToSettings(int event,Object arg)
    {
        synchronized(mSettingsEventHandlerLock) {
            if (mSettingsEventHandler != null)
                mSettingsEventHandler.obtainMessage(event, arg).sendToTarget();
        }
    }

    public static void sendEventToSongList(int event,int arg1,int arg2)
    {
        synchronized(mSongListEventHandlerLock) {
            if (mSongListEventHandler != null)
                mSongListEventHandler.obtainMessage(event, arg1, arg2).sendToTarget();
        }
    }

    public static void sendEventToSongDisplay(int event,int arg1,int arg2)
    {
        synchronized(mSongDisplayEventHandlerLock) {
            if (mSongDisplayEventHandler != null)
                mSongDisplayEventHandler.obtainMessage(event, arg1, arg2).sendToTarget();
        }
    }

    public static void sendEventToSettings(int event,int arg1,int arg2)
    {
        synchronized(mSettingsEventHandlerLock) {
            if (mSettingsEventHandler != null)
                mSettingsEventHandler.obtainMessage(event, arg1, arg2).sendToTarget();
        }
    }

    public static void sendEventToSongList(int event,int arg1,int arg2,Object arg3)
    {
        synchronized(mSongListEventHandlerLock) {
            if (mSongListEventHandler != null)
                mSongListEventHandler.obtainMessage(event, arg1, arg2, arg3).sendToTarget();
        }
    }

    public static void sendEventToSongDisplay(int event,int arg1,int arg2,Object arg3)
    {
        synchronized(mSongDisplayEventHandlerLock) {
            if (mSongDisplayEventHandler != null)
                mSongDisplayEventHandler.obtainMessage(event, arg1, arg2, arg3).sendToTarget();
        }
    }

    public static void sendEventToSettings(int event,int arg1,int arg2,Object arg3)
    {
        synchronized(mSettingsEventHandlerLock) {
            if (mSettingsEventHandler != null)
                mSettingsEventHandler.obtainMessage(event, arg1, arg2, arg3).sendToTarget();
        }
    }
}
