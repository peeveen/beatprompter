package com.stevenfrew.beatprompter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.stevenfrew.beatprompter.bluetooth.BluetoothMessage;
import com.stevenfrew.beatprompter.bluetooth.PauseOnScrollStartMessage;
import com.stevenfrew.beatprompter.bluetooth.QuitSongMessage;
import com.stevenfrew.beatprompter.bluetooth.SetSongTimeMessage;
import com.stevenfrew.beatprompter.bluetooth.ToggleStartStopMessage;
import com.stevenfrew.beatprompter.cache.SongFile;
import com.stevenfrew.beatprompter.midi.ClockSignalGeneratorTask;
import com.stevenfrew.beatprompter.midi.MIDIController;
import com.stevenfrew.beatprompter.midi.StartStopInTask;
import com.stevenfrew.beatprompter.songload.SongLoadTask;
import com.stevenfrew.beatprompter.songload.SongLoaderTask;

public class SongDisplayActivity extends AppCompatActivity implements SensorEventListener
{
    private SongView mSongView=null;
    private static boolean mSongDisplayActive=false;
    private static SongDisplayActivity mSongDisplayInstance;
    private boolean mStartedByBandLeader=false;
    int mPreferredOrientation;
    int mOrientation;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private long mLastOtherPageDownEvent=0;
    private boolean mAnyOtherKeyPageDown=false;
    private boolean mScrollOnProximity;

    public SongDisplayEventHandler mSongDisplayEventHandler;

    ClockSignalGeneratorTask mMidiClockOutTask;
    StartStopInTask mMidiStartStopInTask=new StartStopInTask();
    Thread mMidiClockOutTaskThread=new Thread(mMidiClockOutTask);
    Thread mMidiStartStopInTaskThread=new Thread(mMidiStartStopInTask);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i=getIntent();
        mSongDisplayInstance=this;

//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        // Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener
        mMidiClockOutTask=new ClockSignalGeneratorTask(i.getBooleanExtra("registered",false));

        SharedPreferences sharedPref = BeatPrompterApplication.getPreferences();
        boolean sendMidiClock = sharedPref.getBoolean(getString(R.string.pref_sendMidi_key), false);
        boolean readMidi = sharedPref.getBoolean(getString(R.string.pref_readMidi_key), false);
        mScrollOnProximity=sharedPref.getBoolean(getString(R.string.pref_proximityScroll_key), false);
        // TODO: some sort of normal keyboard support.
        mAnyOtherKeyPageDown=false;//sharedPref.getBoolean(getString(R.string.pref_proximityScroll_key), false);

        Song song= SongLoaderTask.getCurrentSong();
        if(song!=null) {
            mStartedByBandLeader=song.mStartedByBandLeader;
            sendMidiClock |= song.mSendMidiClock;
            int orientation = song.mOrientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                mOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            else
                mOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
            setRequestedOrientation(mOrientation);

            MIDIController.mMIDIOutQueue.addAll(song.mInitialMIDIMessages);
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_song_display);
        mSongView = findViewById(R.id.song_view);
        mSongDisplayEventHandler=new SongDisplayEventHandler(this,mSongView);
        EventHandler.setSongDisplayEventHandler(mSongDisplayEventHandler);
        mSongView.init(this);

        if (sendMidiClock)
            mMidiClockOutTaskThread.start();
        if (readMidi)
            mMidiStartStopInTaskThread.start();

        try {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (mSensorManager != null) {
                mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            }
        }catch(Exception e)
        {
            // Nae sensors.
        }

        // Set up the user interaction to manually show or hide the system UI.
        mSongView.setOnClickListener(view -> {
        });
    }

    public boolean canYieldToExternalTrigger() {
        return mSongView == null || mSongView.canYieldToExternalTrigger();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        mSongDisplayActive=false;
        super.onPause();
        Task.pauseTask(mMidiStartStopInTask,mMidiStartStopInTaskThread);
        Task.pauseTask(mMidiClockOutTask,mMidiClockOutTaskThread);
        if(mSongView!=null)
            mSongView.stop(false);
        if((mSensorManager!=null)&&(mProximitySensor!=null))
            mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        setRequestedOrientation(mPreferredOrientation);
        super.onDestroy();

        Task.stopTask(mMidiClockOutTask,mMidiClockOutTaskThread);
        Task.stopTask(mMidiStartStopInTask,mMidiStartStopInTaskThread);

        if(mSongView!=null) {
            mSongView.stop(true);
            mSongView = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mSongView!=null)
            mSongView.stop(false);
    }

    @Override
    protected void onResume()
    {
        mSongDisplayActive=true;
        super.onResume();
        setRequestedOrientation(mOrientation);
        Task.resumeTask(mMidiStartStopInTask);
        Task.resumeTask(mMidiClockOutTask);
        setRequestedOrientation(mOrientation);
        if((mSensorManager!=null)&&(mProximitySensor!=null))
            mSensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void processBluetoothMessage(BluetoothMessage btm)
    {
        if(mStartedByBandLeader) {
            if (btm instanceof ToggleStartStopMessage)
            {
                ToggleStartStopMessage tssm=(ToggleStartStopMessage)btm;
                if(mSongView!=null) {
                    if (tssm.mTime >= 0)
                        mSongView.setSongTime(tssm.mTime, true,false,true);
                    mSongView.startToggle(null, false, ((ToggleStartStopMessage) btm).mStartState);
                }
            }
            else if (btm instanceof SetSongTimeMessage) {
                if (mSongView != null)
                    mSongView.setSongTime((SetSongTimeMessage) btm);
            }
            else if (btm instanceof PauseOnScrollStartMessage) {
                if(mSongView!=null)
                    mSongView.pauseOnScrollStart();
            }
            else if (btm instanceof QuitSongMessage)
                finish();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                if(mSongView!=null)
                    mSongView.onPageDownKeyPressed();
                return true;
            case KeyEvent.KEYCODE_B:
            case KeyEvent.KEYCODE_PAGE_UP:
                if(mSongView!=null)
                    mSongView.onPageUpKeyPressed();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if(mSongView!=null)
                    mSongView.onLineDownKeyPressed();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if(mSongView!=null)
                    mSongView.onLineUpKeyPressed();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if(mSongView!=null)
                    mSongView.onLeftKeyPressed();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if(mSongView!=null)
                    mSongView.onRightKeyPressed();
                return true;
            default:
                if(mAnyOtherKeyPageDown) {
                    activateOtherPageDown(System.nanoTime());
                    return true;
                }
                else
                    return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(mScrollOnProximity)
        if (mSongView != null){
            long eventTime = sensorEvent.timestamp;
            activateOtherPageDown(eventTime);
        }
    }

    void activateOtherPageDown(long eventTime)
    {
        // Don't allow two of these events to happen within the same second.
        if (eventTime - mLastOtherPageDownEvent > 1000000000) {
            mLastOtherPageDownEvent = eventTime;
            mSongView.onOtherPageDownActivated();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Don't care.
    }

    public void onSongBeat(double bpm)
    {
        if(bpm!=0.0)
            mMidiClockOutTask.setBPM(bpm);
    }

    public void onSongStop()
    {
        Task.stopTask(mMidiClockOutTask,mMidiClockOutTaskThread);
    }

    public static class SongDisplayEventHandler extends EventHandler {
        private SongView mSongView;
        private SongDisplayActivity mActivity;

        SongDisplayEventHandler(SongDisplayActivity activity,SongView songView)
        {
            mActivity=activity;
            mSongView=songView;
        }
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case EventHandler.BLUETOOTH_MESSAGE_RECEIVED:
                    mActivity.processBluetoothMessage((BluetoothMessage)msg.obj);
                    break;
                case EventHandler.MIDI_SET_SONG_POSITION:
                    if(mSongView!=null)
                        mSongView.setSongBeatPosition(msg.arg1,true);
                    else
                        Log.d(BeatPrompterApplication.TAG,"MIDI song position pointer received by SongDisplay before view was created.");
                    break;
                case EventHandler.MIDI_START_SONG:
                    if(mSongView!=null)
                        mSongView.startSong(true,true);
                    else
                        Log.d(BeatPrompterApplication.TAG,"MIDI start signal received by SongDisplay before view was created.");
                    break;
                case EventHandler.MIDI_CONTINUE_SONG:
                    if(mSongView!=null)
                        mSongView.startSong(true,false);
                    else
                        Log.d(BeatPrompterApplication.TAG,"MIDI continue signal received by SongDisplay before view was created.");
                    break;
                case EventHandler.MIDI_STOP_SONG:
                    if(mSongView!=null)
                        mSongView.stopSong(true);
                    else
                        Log.d(BeatPrompterApplication.TAG,"MIDI stop signal received by SongDisplay before view was created.");
                    break;
                case EventHandler.END_SONG:
                    mActivity.setResult(Activity.RESULT_OK);
                    mActivity.finish();
                    break;
                case EventHandler.MIDI_LSB_BANK_SELECT:
                    MIDIController.mMidiBankLSBs[msg.arg1]=(byte)msg.arg2;
                    break;
                case EventHandler.MIDI_MSB_BANK_SELECT:
                    MIDIController.mMidiBankMSBs[msg.arg1]=(byte)msg.arg1;
                    break;
            }
        }
    }

    public static SongInterruptResult interruptCurrentSong(SongLoadTask loadTask, SongFile songToInterruptWith)
    {
        if(mSongDisplayActive)
        {
            Song loadedSong=SongLoaderTask.getCurrentSong();

            // This should never happen, but we'll check just to be sure.
            if(loadedSong==null)
                return SongInterruptResult.NoSongToInterrupt;

            // Trying to interrupt a song with itself is pointless!
            if(loadedSong.mSongFile.mID.equals(songToInterruptWith.mID))
                return SongInterruptResult.NoSongToInterrupt;

            if(mSongDisplayInstance.canYieldToExternalTrigger()) {
                loadedSong.mCancelled = true;
                SongLoadTask.mSongLoadTaskOnResume=loadTask;
                EventHandler.sendEventToSongDisplay(EventHandler.END_SONG);
                return SongInterruptResult.CanInterrupt;
            }
            else {
                SongLoadTask.mSongLoadTaskOnResume = null;
                return SongInterruptResult.CannotInterrupt;
            }
        }
        return SongInterruptResult.NoSongToInterrupt;
    }
}
