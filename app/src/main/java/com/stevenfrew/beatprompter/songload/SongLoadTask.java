package com.stevenfrew.beatprompter.songload;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.EventHandler;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.ScrollingMode;
import com.stevenfrew.beatprompter.Song;
import com.stevenfrew.beatprompter.SongDisplayActivity;
import com.stevenfrew.beatprompter.SongDisplaySettings;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.bluetooth.BluetoothManager;
import com.stevenfrew.beatprompter.bluetooth.ChooseSongMessage;
import com.stevenfrew.beatprompter.cache.SongFile;
import com.stevenfrew.beatprompter.event.CancelEvent;

import java.util.concurrent.Semaphore;

/**
 * This task does not actually load the song. It creates a second task (SongLoaderTask) which uses
 * the SongLoader class internally to load the song.
 * This task deals with the progress dialog UI side of things, and caters for situations where some
 * external event triggers the loading of a song either while a song is currently active, or while
 * a song is already being loaded.
 */
public class SongLoadTask extends AsyncTask<String, Integer, Boolean> {
    private static final String AUTOLOAD_TAG="autoload";
    private static final Object mSongLoadSyncObject=new Object();
    private static SongLoadTask mSongLoadTask=null;

    Semaphore mTaskEndSemaphore=new Semaphore(0);
    boolean mCancelled=false;
    String mProgressTitle="";
    private CancelEvent mCancelEvent=new CancelEvent();
    private SongLoadInfo mSongLoadInfo;
    private ProgressDialog mProgressDialog;
    private SongLoadTaskEventHandler mSongLoadTaskEventHandler;
    private boolean mRegistered;

    public SongLoadTask(SongFile selectedSong, String trackName, ScrollingMode scrollMode, String nextSongName, boolean startedByBandLeader, boolean startedByMidiTrigger, SongDisplaySettings nativeSettings, SongDisplaySettings sourceSettings, boolean registered)
    {
        mSongLoadInfo=new SongLoadInfo(selectedSong,trackName,scrollMode,nextSongName,startedByBandLeader,startedByMidiTrigger,nativeSettings,sourceSettings);
        mSongLoadTaskEventHandler=new SongLoadTaskEventHandler(this);
        mRegistered=registered;
    }

    @Override
    protected Boolean doInBackground(String... paramParams) {
        try
        {
            // The only thing that this "task" does here is attempt to acquire the
            // semaphore. The semaphore is created initially with zero permits, so
            // this will fail/wait until the semaphore is released, which occurs
            // when the handler receives a "completed" or "cancelled" message from
            // the SongLoaderTask.
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
            mProgressDialog.setMessage(mProgressTitle+mSongLoadInfo.getSongFile().mTitle);
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
        mProgressDialog.setMessage(mSongLoadInfo.getSongFile().mTitle);
        mProgressDialog.setMax(mSongLoadInfo.getSongFile().mLines);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Resources.getSystem().getString(android.R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
            mCancelled=true;
            mCancelEvent.set();
        });
        mProgressDialog.show();
    }

    public static void loadSong(SongLoadTask loadTask) {
        synchronized (mSongLoadSyncObject) {
            mSongLoadTask = loadTask;
        }
        mSongLoadTask.loadSong();
    }

    /**
     * This is the entry point for kicking off the loading of a song file.
     */
    private void loadSong()
    {
        // If the song-display activity is currently active, then try to interrupt
        // the current song with this one. If not possible, don't bother.
        if(SongDisplayActivity.mSongDisplayActive) {
            SongList.mSongLoadTaskOnResume=this;
            if(!SongLoadTask.cancelCurrentSong(mSongLoadInfo.getSongFile()))
                SongList.mSongLoadTaskOnResume=null;
            return;
        }

        // Create a bluetooth song-selection message to broadcast to other listeners.
        ChooseSongMessage csm=new ChooseSongMessage(mSongLoadInfo.getSongFile().mTitle,
                mSongLoadInfo.getTrack(),
                mSongLoadInfo.getNativeDisplaySettings().mOrientation,
                mSongLoadInfo.getScrollMode()==ScrollingMode.Beat,
                mSongLoadInfo.getScrollMode()==ScrollingMode.Smooth,
                mSongLoadInfo.getNativeDisplaySettings().mMinFontSize,
                mSongLoadInfo.getNativeDisplaySettings().mMaxFontSize,
                mSongLoadInfo.getNativeDisplaySettings().mScreenWidth,
                mSongLoadInfo.getNativeDisplaySettings().mScreenHeight);
        BluetoothManager.broadcastMessageToClients(csm);

        // Kick off the loading of the new song.
        BeatPrompterApplication.loadSong(mSongLoadInfo,mSongLoadTaskEventHandler,mCancelEvent,mRegistered);
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
                    mSongLoadTask.mProgressTitle= BeatPrompterApplication.getResourceString(R.string.loadingSong);
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

    private static boolean cancelCurrentSong(SongFile songWeWantToInterruptWith)
    {
        Song loadedSong=SongLoaderTask.getCurrentSong();
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

    public static boolean songCurrentlyLoading()
    {
        synchronized (mSongLoadSyncObject) {
            return mSongLoadTask != null;
        }
    }
}


