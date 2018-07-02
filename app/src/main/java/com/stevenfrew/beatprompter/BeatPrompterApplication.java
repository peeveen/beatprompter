package com.stevenfrew.beatprompter;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.stevenfrew.beatprompter.bluetooth.BluetoothManager;
import com.stevenfrew.beatprompter.midi.MIDIController;

public class BeatPrompterApplication extends Application {
    public static final String TAG = "beatprompter";
    public final static String APP_NAME="BeatPrompter";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public void onCreate() {
        super.onCreate();
        MIDIController.initialise(this);
        BluetoothManager.initialise(this);
    }

    public void onTerminate()
    {
        BluetoothManager.shutdown(this);
        MIDIController.shutdown(this);
        super.onTerminate();
    }

}
