package com.stevenfrew.beatprompter;

import android.os.Handler;

import com.stevenfrew.beatprompter.event.CancelEvent;

import java.io.IOException;

class SongLoaderTask extends Task {
    private CancelEvent mCancelEvent=null;
    private SongLoadTask.LoadingSongFile mLoadingSongFile=null;

    private Handler mSongLoadHandler=null;
    private final Object mSongLoadHandlerSync = new Object();
    private final Object mLoadingSongFileSync = new Object();
    private final Object mCancelEventSync = new Object();
    private boolean mRegistered;

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
    private SongLoadTask.LoadingSongFile getLoadingSongFile()
    {
        synchronized (mLoadingSongFileSync)
        {
            SongLoadTask.LoadingSongFile result=mLoadingSongFile;
            mLoadingSongFile=null;
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
        SongLoadTask.LoadingSongFile lsf=getLoadingSongFile();
        if(lsf!=null) {
            System.gc();
            Handler songLoadHandler=getSongLoadHandler();
            CancelEvent cancelEvent = getCancelEvent();
            try {
                SongLoader loader=new SongLoader(lsf,cancelEvent,songLoadHandler,mRegistered);
                Song loadedSong = loader.load();
                if (cancelEvent.isCancelled())
                    songLoadHandler.obtainMessage(EventHandler.SONG_LOAD_CANCELLED).sendToTarget();
                else {
                    SongLoadTask.setCurrentSong(loadedSong);
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
    void loadSong(SongLoadTask.LoadingSongFile lsf, Handler handler, CancelEvent cancelEvent,boolean registered)
    {
        setSongLoadHandler(handler);
        synchronized (mLoadingSongFileSync)
        {
            CancelEvent existingCancelEvent=getCancelEvent();
            if(existingCancelEvent!=null)
                existingCancelEvent.set();
            setCancelEvent(cancelEvent);
            mRegistered=registered;
            mLoadingSongFile=lsf;
        }
    }
}


