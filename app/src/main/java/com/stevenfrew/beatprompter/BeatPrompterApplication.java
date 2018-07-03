package com.stevenfrew.beatprompter;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;

import com.stevenfrew.beatprompter.bluetooth.BluetoothManager;
import com.stevenfrew.beatprompter.midi.MIDIController;

public class BeatPrompterApplication extends Application {
    public static final String TAG = "beatprompter";
    public final static String APP_NAME="BeatPrompter";
    private static Application mApp;
    private final static String SHARED_PREFERENCES_ID="beatPrompterSharedPreferences";

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
    }

    public void onTerminate()
    {
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
}
