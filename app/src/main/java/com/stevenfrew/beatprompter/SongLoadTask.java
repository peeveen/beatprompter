package com.stevenfrew.beatprompter;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import com.stevenfrew.beatprompter.bluetooth.BluetoothManager;
import com.stevenfrew.beatprompter.bluetooth.ChooseSongMessage;
import com.stevenfrew.beatprompter.event.CancelEvent;

import java.util.concurrent.Semaphore;

class SongLoadTask extends AsyncTask<String, Integer, Boolean> {
    Semaphore mTaskEndSemaphore=new Semaphore(0);
    boolean mCancelled=false;
    String mProgressTitle="";

    static final Object mSongLoadSyncObject=new Object();
    static SongLoadTask mSongLoadTask=null;
    static LoadingSongFile mSongToLoadOnResume=null;

    private CancelEvent mCancelEvent=new CancelEvent();

    private LoadingSongFile mLoadingSongFile;
    private ProgressDialog mProgressDialog;
    private SongLoadTaskEventHandler mSongLoadTaskEventHandler;

    SongLoadTask(LoadingSongFile lsf)
    {
        mLoadingSongFile=lsf;
        mSongLoadTaskEventHandler=new SongLoadTaskEventHandler(this);
    }

    @Override
    protected Boolean doInBackground(String... paramParams) {
        try
        {
            mTaskEndSemaphore.acquire();
        }
        catch(InterruptedException ignored)
        {
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if(values.length>1) {
            mProgressDialog.setMessage(mProgressTitle+mLoadingSongFile.mSongFile.mTitle);
            mProgressDialog.setMax(values[1]);
            mProgressDialog.setProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Boolean b) {
        Log.d(BeatPrompterApplication.AUTOLOAD_TAG,"In load task PostExecute.");
        super.onPostExecute(b);
        if (mProgressDialog!=null) {
            mProgressDialog.dismiss();}
        if(mCancelled)
            Log.d(BeatPrompterApplication.AUTOLOAD_TAG,"Song load was cancelled.");
        else
            Log.d(BeatPrompterApplication.AUTOLOAD_TAG,"Song loaded successfully.");
        Log.d(BeatPrompterApplication.AUTOLOAD_TAG,"Song loaded successfully.");
        synchronized (mSongLoadSyncObject)
        {
            mSongLoadTask=null;
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialog(SongList.mSongListInstance);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMessage(mLoadingSongFile.mSongFile.mTitle);
        mProgressDialog.setMax(mLoadingSongFile.mSongFile.mLines);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Resources.getSystem().getString(android.R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
            mCancelled=true;
            mCancelEvent.set();
        });
        mProgressDialog.show();
    }

    void loadSong()
    {
        if(SongDisplayActivity.mSongDisplayActive) {
            mSongToLoadOnResume=mLoadingSongFile;
            if(!BeatPrompterApplication.cancelCurrentSong(mLoadingSongFile.mSongFile))
                mSongToLoadOnResume=null;
            return;
        }

        BluetoothManager.broadcastMessageToClients(new ChooseSongMessage(mLoadingSongFile));
        SongList.mSongLoaderTask.setSongToLoad(mLoadingSongFile,mSongLoadTaskEventHandler,mCancelEvent);
        this.execute();
    }

    public static class SongLoadTaskEventHandler extends EventHandler {

        SongLoadTask mSongLoadTask;
        SongLoadTaskEventHandler(SongLoadTask songLoadTask)
        {
            mSongLoadTask=songLoadTask;
        }
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case EventHandler.SONG_LOAD_COMPLETED:
                    mSongLoadTask.mTaskEndSemaphore.release();
                    EventHandler.sendEventToSongList(EventHandler.SONG_LOAD_COMPLETED);
                    break;
                case EventHandler.SONG_LOAD_CANCELLED:
                    mSongLoadTask.mCancelled=true;
                    mSongLoadTask.mTaskEndSemaphore.release();
                    break;
                case EventHandler.SONG_LOAD_LINE_READ:
                    mSongLoadTask.mProgressTitle=SongList.mSongListInstance.getString(R.string.loadingSong);
                    mSongLoadTask.publishProgress(msg.arg1,msg.arg2);
                    break;
                case EventHandler.SONG_LOAD_LINE_PROCESSED:
                    mSongLoadTask.mProgressTitle=SongList.mSongListInstance.getString(R.string.processingSong);
                    mSongLoadTask.publishProgress(msg.arg1,msg.arg2);
                    break;
                case EventHandler.SONG_LOAD_FAILED:
                    EventHandler.sendEventToSongList(EventHandler.SONG_LOAD_FAILED,msg.obj);
                    break;
            }
        }
    }
}


