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
    private void setLoadingSongFile(SongLoadTask.LoadingSongFile lsf,CancelEvent cancelEvent)
    {
        synchronized (mLoadingSongFileSync)
        {
            CancelEvent existingCancelEvent=getCancelEvent();
            if(existingCancelEvent!=null)
                existingCancelEvent.set();
            setCancelEvent(cancelEvent);
            mLoadingSongFile=lsf;
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

    SongLoaderTask()
    {
        super(true);
    }
    public void doWork()
    {
        SongLoadTask.LoadingSongFile lsf=getLoadingSongFile();
        if(lsf!=null) {
            System.gc();
            Handler songLoadHandler=getSongLoadHandler();
            CancelEvent cancelEvent = getCancelEvent();
            try {
                Song loadingSong = lsf.load(cancelEvent, songLoadHandler,SongList.getMIDIAliases());
                if (cancelEvent.isCancelled())
                    songLoadHandler.obtainMessage(EventHandler.SONG_LOAD_CANCELLED).sendToTarget();
                else {
                    SongLoadTask.setCurrentSong(loadingSong);
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
    void setSongToLoad(SongLoadTask.LoadingSongFile lsf, Handler handler, CancelEvent cancelEvent)
    {
        setSongLoadHandler(handler);
        setLoadingSongFile(lsf,cancelEvent);
    }
}


