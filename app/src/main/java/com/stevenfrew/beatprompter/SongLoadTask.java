package com.stevenfrew.beatprompter;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.stevenfrew.beatprompter.bluetooth.BluetoothManager;
import com.stevenfrew.beatprompter.bluetooth.ChooseSongMessage;
import com.stevenfrew.beatprompter.cache.SongFile;
import com.stevenfrew.beatprompter.event.CancelEvent;
import com.stevenfrew.beatprompter.midi.Alias;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

class SongLoadTask extends AsyncTask<String, Integer, Boolean> {
    private static Song mCurrentSong=null;
    private final static Object mCurrentSongSync=new Object();
    private static final String AUTOLOAD_TAG="autoload";
    static final Object mSongLoadSyncObject=new Object();
    static SongLoadTask mSongLoadTask=null;

    Semaphore mTaskEndSemaphore=new Semaphore(0);
    boolean mCancelled=false;
    String mProgressTitle="";
    private CancelEvent mCancelEvent=new CancelEvent();
    private LoadingSongFile mLoadingSongFile;
    private ProgressDialog mProgressDialog;
    private SongLoadTaskEventHandler mSongLoadTaskEventHandler;

    SongLoadTask(SongFile selectedSong, String trackName, ScrollingMode scrollMode, String nextSongName, boolean startedByBandLeader, boolean startedByMidiTrigger, SongDisplaySettings nativeSettings, SongDisplaySettings sourceSettings,boolean demo)
    {
        mLoadingSongFile=new LoadingSongFile(selectedSong,trackName,scrollMode,nextSongName,startedByBandLeader,startedByMidiTrigger,nativeSettings,sourceSettings,demo);
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
        Log.d(AUTOLOAD_TAG,"In load task PostExecute.");
        super.onPostExecute(b);
        if (mProgressDialog!=null) {
            mProgressDialog.dismiss();}
        if(mCancelled)
            Log.d(AUTOLOAD_TAG,"Song load was cancelled.");
        else
            Log.d(AUTOLOAD_TAG,"Song loaded successfully.");
        Log.d(AUTOLOAD_TAG,"Song loaded successfully.");
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
            SongList.mSongLoadTaskOnResume=this;
            if(!SongLoadTask.cancelCurrentSong(mLoadingSongFile.mSongFile))
                SongList.mSongLoadTaskOnResume=null;
            return;
        }

        ChooseSongMessage csm=new ChooseSongMessage(mLoadingSongFile.mSongFile.mTitle,
                mLoadingSongFile.mTrack,
                mLoadingSongFile.mNativeDisplaySettings.mOrientation,
                mLoadingSongFile.mScrollMode==ScrollingMode.Beat,
                mLoadingSongFile.mScrollMode==ScrollingMode.Smooth,
                mLoadingSongFile.mNativeDisplaySettings.mMinFontSize,
                mLoadingSongFile.mNativeDisplaySettings.mMaxFontSize,
                mLoadingSongFile.mNativeDisplaySettings.mScreenWidth,
                mLoadingSongFile.mNativeDisplaySettings.mScreenHeight);
        BluetoothManager.broadcastMessageToClients(csm);

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
                    mSongLoadTask.mProgressTitle=BeatPrompterApplication.getResourceString(R.string.loadingSong);
                    mSongLoadTask.publishProgress(msg.arg1,msg.arg2);
                    break;
                case EventHandler.SONG_LOAD_LINE_PROCESSED:
                    mSongLoadTask.mProgressTitle=BeatPrompterApplication.getResourceString(R.string.processingSong);
                    mSongLoadTask.publishProgress(msg.arg1,msg.arg2);
                    break;
                case EventHandler.SONG_LOAD_FAILED:
                    EventHandler.sendEventToSongList(EventHandler.SONG_LOAD_FAILED,msg.obj);
                    break;
            }
        }
    }

    class LoadingSongFile {
        SongFile mSongFile;
        String mTrack;
        ScrollingMode mScrollMode;
        SongDisplaySettings mNativeDisplaySettings;
        private boolean mStartedByBandLeader;
        private boolean mStartedByMIDITrigger;
        private String mNextSong;
        private SongDisplaySettings mSourceDisplaySettings;
        private boolean mRegistered;

        LoadingSongFile(SongFile songFile, String track, ScrollingMode mode,String nextSong,boolean startedByBandLeader,boolean startedByMidiTrigger,SongDisplaySettings nativeSettings,SongDisplaySettings sourceSettings,boolean registered)
        {
            mSongFile=songFile;
            mStartedByMIDITrigger=startedByMidiTrigger;
            mTrack=track;
            mRegistered=registered;
            mScrollMode=mode;
            mNextSong=nextSong;
            mStartedByBandLeader=startedByBandLeader;
            mNativeDisplaySettings=nativeSettings;
            mSourceDisplaySettings=sourceSettings;
        }
        Song load(CancelEvent cancelEvent, Handler handler, ArrayList<Alias> midiAliases) throws IOException
        {
            SongLoader loader=new SongLoader(mSongFile,mScrollMode);
            return loader.load(mTrack,mRegistered,mStartedByBandLeader,mNextSong,cancelEvent,handler,mStartedByMIDITrigger,midiAliases,mNativeDisplaySettings,mSourceDisplaySettings);
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

    private static boolean cancelCurrentSong(SongFile songWeWantToInterruptWith)
    {
        Song loadedSong=getCurrentSong();
        if(loadedSong!=null)
            if(SongDisplayActivity.mSongDisplayActive)
                if(!loadedSong.mSongFile.mTitle.equals(songWeWantToInterruptWith.mTitle))
                    if(SongDisplayActivity.mSongDisplayInstance.canYieldToMIDITrigger()) {
                        loadedSong.mCancelled = true;
                        EventHandler.sendEventToSongDisplay(EventHandler.END_SONG);
                    }
                    else
                        return false;
                else
                    // Trying to interrupt a song with itself is pointless!
                    return false;
        return true;
    }
}


