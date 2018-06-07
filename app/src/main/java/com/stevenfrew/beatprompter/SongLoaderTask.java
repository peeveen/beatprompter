package com.stevenfrew.beatprompter;

import android.content.Context;
import android.os.Handler;

import java.io.IOException;

class SongLoaderTask extends Task {
    CancelEvent mCancelEvent=null;
    LoadingSongFile mLoadingSongFile=null;

    Handler mSongLoadHandler=null;
    final Object mSongLoadHandlerSync = new Object();
    final Object mLoadingSongFileSync = new Object();
    final Object mCancelEventSync = new Object();

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
    private LoadingSongFile getLoadingSongFile()
    {
        synchronized (mLoadingSongFileSync)
        {
            LoadingSongFile result=mLoadingSongFile;
            mLoadingSongFile=null;
            return result;
        }
    }
    private void setLoadingSongFile(LoadingSongFile lsf,CancelEvent cancelEvent)
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
    void doWork()
    {
        LoadingSongFile lsf=getLoadingSongFile();
        if(lsf!=null) {
            System.gc();
            Handler songLoadHandler=getSongLoadHandler();
            CancelEvent cancelEvent = getCancelEvent();
            try {
                Song loadingSong = lsf.load(SongList.isFullVersionUnlocked(), cancelEvent, songLoadHandler,SongList.getMIDIAliases());
                if (cancelEvent.isCancelled())
                    songLoadHandler.obtainMessage(BeatPrompterApplication.SONG_LOAD_CANCELLED).sendToTarget();
                else {
                    BeatPrompterApplication.setCurrentSong(loadingSong);
                    songLoadHandler.obtainMessage(BeatPrompterApplication.SONG_LOAD_COMPLETED).sendToTarget();
                }
            }
            catch(IOException ioe)
            {
                songLoadHandler.obtainMessage(BeatPrompterApplication.SONG_LOAD_FAILED,ioe.getMessage()).sendToTarget();
            }
            System.gc();
        }
        else
            try {
                // Nothing to do, wait a bit
                Thread.sleep(250);
            }
            catch(InterruptedException ie)
            {
            }
    }
    void setSongToLoad(LoadingSongFile lsf,Handler handler,CancelEvent cancelEvent)
    {
        setSongLoadHandler(handler);
        setLoadingSongFile(lsf,cancelEvent);
    }
}


