package com.stevenfrew.beatprompter;

import android.os.Handler;

import com.stevenfrew.beatprompter.event.CancelEvent;

import java.io.IOException;

class SongLoaderTask extends Task {
    private CancelEvent mCancelEvent=null;
    private SongLoadInfo mSongLoadInfo=null;
    private static Song mCurrentSong=null;
    private Handler mSongLoadHandler=null;
    private boolean mRegistered;

    private final static Object mCurrentSongSync=new Object();
    private final Object mSongLoadHandlerSync = new Object();
    private final Object mLoadingSongFileSync = new Object();
    private final Object mCancelEventSync = new Object();

    SongLoaderTask()
    {
        super(true);
    }

    private Handler getSongLoadHandler()
    {
        synchronized (mSongLoadHandlerSync)
        {
            return mSongLoadHandler;
        }
    }
    private void setSongLoadHandler(Handler handler)
    {
        synchronized (mSongLoadHandlerSync)
        {
            mSongLoadHandler=handler;
        }
    }


    static Song getCurrentSong()
    {
        synchronized (mCurrentSongSync)
        {
            return mCurrentSong;
        }
    }

    static void setCurrentSong(Song song)
    {
        synchronized (mCurrentSongSync)
        {
            mCurrentSong=song;
            System.gc();
        }
    }

    private SongLoadInfo getLoadingSongFile()
    {
        synchronized (mLoadingSongFileSync)
        {
            SongLoadInfo result=mSongLoadInfo;
            mSongLoadInfo=null;
            return result;
        }
    }
    private CancelEvent getCancelEvent()
    {
        synchronized (mCancelEventSync)
        {
            return mCancelEvent;
        }
    }
    private void setCancelEvent(CancelEvent cancelEvent)
    {
        synchronized (mCancelEventSync)
        {
            mCancelEvent=cancelEvent;
        }
    }

    public void doWork()
    {
        SongLoadInfo sli=getLoadingSongFile();
        if(sli!=null) {
            System.gc();
            Handler songLoadHandler=getSongLoadHandler();
            CancelEvent cancelEvent = getCancelEvent();
            try {
                SongLoader loader=new SongLoader(sli,cancelEvent,songLoadHandler,mRegistered);
                Song loadedSong = loader.load();
                if (cancelEvent.isCancelled())
                    songLoadHandler.obtainMessage(EventHandler.SONG_LOAD_CANCELLED).sendToTarget();
                else {
                    setCurrentSong(loadedSong);
                    songLoadHandler.obtainMessage(EventHandler.SONG_LOAD_COMPLETED).sendToTarget();
                }
            }
            catch(IOException ioe)
            {
                songLoadHandler.obtainMessage(EventHandler.SONG_LOAD_FAILED,ioe.getMessage()).sendToTarget();
            }
            System.gc();
        }
        else
            try {
                // Nothing to do, wait a bit
                Thread.sleep(250);
            }
            catch(InterruptedException ignored)
            {
            }
    }
    void loadSong(SongLoadInfo sli, Handler handler, CancelEvent cancelEvent,boolean registered)
    {
        setSongLoadHandler(handler);
        synchronized (mLoadingSongFileSync)
        {
            CancelEvent existingCancelEvent=getCancelEvent();
            if(existingCancelEvent!=null)
                existingCancelEvent.set();
            setCancelEvent(cancelEvent);
            mRegistered=registered;
            mSongLoadInfo=sli;
        }
    }
}


