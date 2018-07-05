package com.stevenfrew.beatprompter;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;

import com.stevenfrew.beatprompter.bluetooth.BluetoothManager;
import com.stevenfrew.beatprompter.event.CancelEvent;
import com.stevenfrew.beatprompter.midi.MIDIController;
import com.stevenfrew.beatprompter.songload.SongLoadInfo;
import com.stevenfrew.beatprompter.songload.SongLoaderTask;

public class BeatPrompterApplication extends Application {
    public static final String TAG = "beatprompter";
    public final static String APP_NAME="BeatPrompter";
    private static Application mApp;
    private final static String SHARED_PREFERENCES_ID="beatPrompterSharedPreferences";
    private static SongLoaderTask mSongLoaderTask = new SongLoaderTask();
    private Thread mSongLoaderTaskThread = new Thread(mSongLoaderTask);

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public void onCreate() {
        super.onCreate();
        mApp=this;
        MIDIController.initialise(this);
        BluetoothManager.initialise(this);
        mSongLoaderTaskThread.start();
        Task.resumeTask(mSongLoaderTask);
    }

    public void onTerminate()
    {
        Task.stopTask(mSongLoaderTask,mSongLoaderTaskThread);
        BluetoothManager.shutdown(this);
        MIDIController.shutdown(this);
        super.onTerminate();
    }

    public static String getResourceString(int resID)
    {
        return mApp.getString(resID);
    }

    public static String getResourceString(int resID,Object... args)
    {
        return mApp.getString(resID,args);
    }

    public static SharedPreferences getPreferences()
    {
        return PreferenceManager.getDefaultSharedPreferences(mApp);
    }

    public static SharedPreferences getPrivatePreferences()
    {
        return mApp.getSharedPreferences(SHARED_PREFERENCES_ID, Context.MODE_PRIVATE);
    }

    public static AssetManager getAssetManager()
    {
        return mApp.getAssets();
    }

    public static Context getContext()
    {
        return mApp.getApplicationContext();
    }

    public static void loadSong(SongLoadInfo sli, Handler handler, CancelEvent cancelEvent, boolean registered)
    {
        mSongLoaderTask.loadSong(sli,handler,cancelEvent,registered);
    }
}
