package com.stevenfrew.beatprompter;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.stevenfrew.beatprompter.bluetooth.BluetoothManager;
import com.stevenfrew.beatprompter.cache.SongFile;
import com.stevenfrew.beatprompter.midi.IncomingMessage;
import com.stevenfrew.beatprompter.midi.OutgoingMessage;

import java.util.concurrent.ArrayBlockingQueue;

public class BeatPrompterApplication extends Application {
    public static final String TAG = "beatprompter";
    public static final String MIDI_TAG = "midi";
    public static final String AUTOLOAD_TAG = "autoload";
    public final static String APP_NAME="BeatPrompter";
    public final static String SHARED_PREFERENCES_ID="beatPrompterSharedPreferences";

    public static final int MIDI_QUEUE_SIZE=1024;

    public static ArrayBlockingQueue<OutgoingMessage> mMIDIOutQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);
    public static ArrayBlockingQueue<IncomingMessage> mMIDISongDisplayInQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);
    public static ArrayBlockingQueue<IncomingMessage> mMIDISongListInQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);

    static byte[] mMidiBankMSBs=new byte[16];
    static byte[] mMidiBankLSBs=new byte[16];

    private static Song mCurrentSong=null;
    private final static Object mCurrentSongSync=new Object();

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

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    static boolean cancelCurrentSong(SongFile songWeWantToInterruptWith)
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


    public void onCreate() {
        super.onCreate();
        BluetoothManager.initialise(this);
    }

    public void onTerminate()
    {
        BluetoothManager.shutdown(this);
        super.onTerminate();
    }

}
