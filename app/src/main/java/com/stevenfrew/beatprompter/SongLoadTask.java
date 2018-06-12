package com.stevenfrew.beatprompter;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.stevenfrew.beatprompter.bluetooth.ChooseSongMessage;
import com.stevenfrew.beatprompter.event.CancelEvent;

import java.util.concurrent.Semaphore;

class SongLoadTask extends AsyncTask<String, Integer, Boolean> {
    Semaphore mTaskEndSemaphore=new Semaphore(0);
    static final Object mSongLoadSyncObject=new Object();
    static SongLoadTask mSongLoadTask=null;
    static LoadingSongFile mSongToLoadOnResume=null;

    CancelEvent mCancelEvent=new CancelEvent();
    boolean mCancelled=false;
    String mProgressTitle="";

    SongLoadTask(LoadingSongFile lsf,Handler handler)
    {
        mLoadingSongFile=lsf;
        this.mSongListHandler=handler;
    }
    LoadingSongFile mLoadingSongFile=null;
    ProgressDialog mProgressDialog;
    Handler mSongListHandler=null;

    Handler mSongLoadHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case BeatPrompterApplication.SONG_LOAD_COMPLETED:
                    mTaskEndSemaphore.release();
                    mSongListHandler.obtainMessage(BeatPrompterApplication.SONG_LOAD_COMPLETED).sendToTarget();
                    break;
                case BeatPrompterApplication.SONG_LOAD_CANCELLED:
                    mCancelled=true;
                    mTaskEndSemaphore.release();
                    break;
                case BeatPrompterApplication.SONG_LOAD_LINE_READ:
                    mProgressTitle=SongList.getContext().getString(R.string.loadingSong);
                    publishProgress(msg.arg1,msg.arg2);
                    break;
                case BeatPrompterApplication.SONG_LOAD_LINE_PROCESSED:
                    mProgressTitle=SongList.getContext().getString(R.string.processingSong);
                    publishProgress(msg.arg1,msg.arg2);
                    break;
                case BeatPrompterApplication.SONG_LOAD_FAILED:
                    mSongListHandler.obtainMessage(BeatPrompterApplication.SONG_LOAD_FAILED,msg.obj).sendToTarget();
                    break;
            }
        }
    };

    @Override
    protected Boolean doInBackground(String... paramParams) {
        try
        {
            mTaskEndSemaphore.acquire();
        }
        catch(InterruptedException ie)
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
        mProgressDialog = new ProgressDialog(SongList.getContext());
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMessage(mLoadingSongFile.mSongFile.mTitle);
        mProgressDialog.setMax(mLoadingSongFile.mSongFile.mLines);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Resources.getSystem().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mCancelled=true;
                mCancelEvent.set();
            }
        });
        mProgressDialog.show();
    }

    void loadSong(BeatPrompterApplication app)
    {
        if(SongDisplayActivity.mSongDisplayActive) {
            mSongToLoadOnResume=mLoadingSongFile;
            if(!BeatPrompterApplication.cancelCurrentSong(mLoadingSongFile.mSongFile))
                mSongToLoadOnResume=null;
            return;
        }

        app.broadcastMessageToClients(new ChooseSongMessage(mLoadingSongFile));
        SongList.mSongLoaderTask.setSongToLoad(mLoadingSongFile,mSongLoadHandler,mCancelEvent);
        this.execute();
    }
}


