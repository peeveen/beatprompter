package com.stevenfrew.beatprompter.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Message
import android.os.ParcelUuid
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.comm.bluetooth.message.ToggleStartStopMessage
import com.stevenfrew.beatprompter.comm.midi.ClockSignalGeneratorTask
import com.stevenfrew.beatprompter.comm.midi.MIDIController
import com.stevenfrew.beatprompter.song.load.SongInterruptResult
import com.stevenfrew.beatprompter.song.load.SongLoadTask
import com.stevenfrew.beatprompter.song.load.SongLoaderTask

class SongDisplayActivity : AppCompatActivity(), SensorEventListener {
    private var mSongView: SongView? = null
    private var mStartedByBandLeader = false
    private var mPreferredOrientation: Int = 0
    private var mOrientation: Int = 0

    private var mSensorManager: SensorManager? = null
    private var mProximitySensor: Sensor? = null
    private var mScrollOnProximity: Boolean = false

    private var mLastOtherPageDownEvent: Long = 0
    private var mAnyOtherKeyPageDown = false

    private lateinit var mSongDisplayEventHandler: SongDisplayEventHandler

    private val mMidiClockOutTask= ClockSignalGeneratorTask()
    private val mMidiClockOutTaskThread=Thread(mMidiClockOutTask)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Any config change, go back to the song list.
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSongDisplayInstance = this

        val sharedPref = BeatPrompterApplication.preferences
        var sendMidiClock = sharedPref.getBoolean(getString(R.string.pref_sendMidi_key), false)
        mScrollOnProximity = sharedPref.getBoolean(getString(R.string.pref_proximityScroll_key), false)
        // NICETOHAVE: some sort of normal keyboard support.
        mAnyOtherKeyPageDown = false//sharedPref.getBoolean(getString(R.string.pref_proximityScroll_key), false);

        setContentView(R.layout.activity_song_display)
        val potentiallyNullSongView: SongView? = findViewById(R.id.song_view)
        val songView=potentiallyNullSongView?:return
        mSongView=songView

        val song = SongLoaderTask.currentSong?:return
        val loadID: ParcelUuid =intent.extras?.get("loadID") as ParcelUuid
        if(song.mLoadID != loadID.uuid)
            finish()

        mStartedByBandLeader = song.mStartedByBandLeader
        sendMidiClock = sendMidiClock or song.mSendMIDIClock
        val orientation = song.mDisplaySettings.mOrientation
        mOrientation = if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        requestedOrientation = mOrientation

        song.mInitialMIDIMessages.forEach{MIDIController.mMIDIOutQueue.put(it)}

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        mSongDisplayEventHandler = SongDisplayEventHandler(this, mSongView)
        EventHandler.setSongDisplayEventHandler(mSongDisplayEventHandler)
        songView.init(this,song)

        if (sendMidiClock)
            mMidiClockOutTaskThread.start()

        try {
            mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            if (mSensorManager != null) {
                mProximitySensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            }
        } catch (e: Exception) {
            // Nae sensors.
        }

        // Set up the user interaction to manually show or hide the system UI.
        mSongView!!.setOnClickListener { _ -> }
    }

    fun canYieldToExternalTrigger(): Boolean {
        return mSongView == null || mSongView!!.canYieldToExternalTrigger()
    }

    override fun onPause() {
        mSongDisplayActive = false
        super.onPause()
        Task.pauseTask(mMidiClockOutTask, mMidiClockOutTaskThread)
        if (mSongView != null)
            mSongView!!.stop(false)
        if (mSensorManager != null && mProximitySensor != null)
            mSensorManager!!.unregisterListener(this)
    }

    override fun onDestroy() {
        requestedOrientation = mPreferredOrientation
        super.onDestroy()

        Task.stopTask(mMidiClockOutTask, mMidiClockOutTaskThread)

        if (mSongView != null) {
            mSongView!!.stop(true)
            mSongView = null
        }
    }

    override fun onStop() {
        super.onStop()
        if (mSongView != null)
            mSongView!!.stop(false)
    }

    override fun onResume() {
        mSongDisplayActive = true
        super.onResume()
        requestedOrientation = mOrientation
        Task.resumeTask(mMidiClockOutTask)
        requestedOrientation = mOrientation
        if (mSensorManager != null && mProximitySensor != null)
            mSensorManager!!.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_PAGE_DOWN -> {
                if (mSongView != null)
                    mSongView!!.onPageDownKeyPressed()
                return true
            }
            KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_PAGE_UP -> {
                if (mSongView != null)
                    mSongView!!.onPageUpKeyPressed()
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (mSongView != null)
                    mSongView!!.onLineDownKeyPressed()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (mSongView != null)
                    mSongView!!.onLineUpKeyPressed()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (mSongView != null)
                    mSongView!!.onLeftKeyPressed()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (mSongView != null)
                    mSongView!!.onRightKeyPressed()
                return true
            }
            else -> return if (mAnyOtherKeyPageDown) {
                activateOtherPageDown(System.nanoTime())
                true
            } else
                super.onKeyUp(keyCode, event)
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        if (mScrollOnProximity)
            if (mSongView != null)
                activateOtherPageDown(sensorEvent.timestamp)
    }

    private fun activateOtherPageDown(eventTime: Long) {
        // Don't allow two of these events to happen within the same second.
        if (eventTime - mLastOtherPageDownEvent > 1000000000) {
            mLastOtherPageDownEvent = eventTime
            mSongView!!.onOtherPageDownActivated()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {
        // Don't care.
    }

    fun onSongBeat(bpm: Double) {
        if (bpm != 0.0)
            mMidiClockOutTask.setBPM(bpm)
    }

    fun onSongStop() {
        Task.stopTask(mMidiClockOutTask, mMidiClockOutTaskThread)
    }

    class SongDisplayEventHandler internal constructor(private val mActivity: SongDisplayActivity, private val mSongView: SongView?) : EventHandler() {
        override fun handleMessage(msg: Message) {
            if(mSongDisplayActive)
                when (msg.what) {
                    BLUETOOTH_PAUSE_ON_SCROLL_START -> mSongView?.pauseOnScrollStart()
                    BLUETOOTH_QUIT_SONG -> mActivity.finish()
                    BLUETOOTH_SET_SONG_TIME -> mSongView?.setSongTime(msg.obj as Long, true, false, true,true)
                    BLUETOOTH_TOGGLE_START_STOP -> mSongView?.processBluetoothToggleStartStopMessage(msg.obj as ToggleStartStopMessage.StartStopToggleInfo)
                    MIDI_SET_SONG_POSITION -> mSongView?.setSongBeatPosition(msg.arg1, true)
                            ?: Log.d(BeatPrompterApplication.TAG, "MIDI song position pointer received by SongDisplay before view was created.")
                    MIDI_START_SONG -> mSongView?.startSong(true, true)
                            ?: Log.d(BeatPrompterApplication.TAG, "MIDI start signal received by SongDisplay before view was created.")
                    MIDI_CONTINUE_SONG -> mSongView?.startSong(true, false)
                            ?: Log.d(BeatPrompterApplication.TAG, "MIDI continue signal received by SongDisplay before view was created.")
                    MIDI_STOP_SONG -> mSongView?.stopSong(true)
                            ?: Log.d(BeatPrompterApplication.TAG, "MIDI stop signal received by SongDisplay before view was created.")
                    END_SONG -> {
                        mActivity.setResult(Activity.RESULT_OK)
                        mActivity.finish()
                    }
                }
        }
    }

    companion object {
        var mSongDisplayActive = false
        private lateinit var mSongDisplayInstance: SongDisplayActivity

        fun interruptCurrentSong(loadTask: SongLoadTask, songToInterruptWith: SongFile): SongInterruptResult {
            if (mSongDisplayActive) {
                val loadedSong = SongLoaderTask.currentSong
                        ?: return SongInterruptResult.NoSongToInterrupt

                // The first one should never happen, but we'll check just to be sure.
                // Trying to interrupt a song with itself is pointless!
                return when {
                    loadedSong.mSongFile.mID == songToInterruptWith.mID -> SongInterruptResult.NoSongToInterrupt
                    mSongDisplayInstance.canYieldToExternalTrigger() -> {
                        loadedSong.mCancelled = true
                        SongLoadTask.mSongLoadTaskOnResume = loadTask
                        EventHandler.sendEventToSongDisplay(EventHandler.END_SONG)
                        SongInterruptResult.CanInterrupt
                    }
                    else -> {
                        SongLoadTask.mSongLoadTaskOnResume = null
                        SongInterruptResult.CannotInterrupt
                    }
                }
            }
            return SongInterruptResult.NoSongToInterrupt
        }
    }
}
