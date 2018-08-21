package com.stevenfrew.beatprompter

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
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import com.stevenfrew.beatprompter.bluetooth.*
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.midi.ClockSignalGeneratorTask
import com.stevenfrew.beatprompter.midi.MIDIController
import com.stevenfrew.beatprompter.midi.StartStopInTask
import com.stevenfrew.beatprompter.songload.SongLoadTask
import com.stevenfrew.beatprompter.songload.SongLoaderTask

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

    private lateinit var mMidiClockOutTask: ClockSignalGeneratorTask
    private lateinit var mMidiClockOutTaskThread:Thread

    private var mMidiStartStopInTask = StartStopInTask()
    private var mMidiStartStopInTaskThread = Thread(mMidiStartStopInTask)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Any config change, go back to the song list.
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val i = intent
        mSongDisplayInstance = this

        //        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        // Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener

        mMidiClockOutTask = ClockSignalGeneratorTask(i.getBooleanExtra("registered", false))
        mMidiClockOutTaskThread=Thread(mMidiClockOutTask)

        val sharedPref = BeatPrompterApplication.preferences
        var sendMidiClock = sharedPref.getBoolean(getString(R.string.pref_sendMidi_key), false)
        val readMidi = sharedPref.getBoolean(getString(R.string.pref_readMidi_key), false)
        mScrollOnProximity = sharedPref.getBoolean(getString(R.string.pref_proximityScroll_key), false)
        // TODO: some sort of normal keyboard support.
        mAnyOtherKeyPageDown = false//sharedPref.getBoolean(getString(R.string.pref_proximityScroll_key), false);

        setContentView(R.layout.activity_song_display)
        val potentiallyNullSongView:SongView? = findViewById(R.id.song_view)
        val songView=potentiallyNullSongView?:return
        mSongView=songView

        val song = SongLoaderTask.currentSong?:return

        mStartedByBandLeader = song.mStartedByBandLeader
        sendMidiClock = sendMidiClock or song.mSendMIDIClock
        val orientation = song.mDisplaySettings.mOrientation
        mOrientation = if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        requestedOrientation = mOrientation

        MIDIController.mMIDIOutQueue.addAll(song.mInitialMIDIMessages)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        mSongDisplayEventHandler = SongDisplayEventHandler(this, mSongView)
        EventHandler.setSongDisplayEventHandler(mSongDisplayEventHandler)
        songView.init(this,song)

        if (sendMidiClock)
            mMidiClockOutTaskThread.start()
        if (readMidi)
            mMidiStartStopInTaskThread.start()

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
        Task.pauseTask(mMidiStartStopInTask, mMidiStartStopInTaskThread)
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
        Task.stopTask(mMidiStartStopInTask, mMidiStartStopInTaskThread)

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
        Task.resumeTask(mMidiStartStopInTask)
        Task.resumeTask(mMidiClockOutTask)
        requestedOrientation = mOrientation
        if (mSensorManager != null && mProximitySensor != null)
            mSensorManager!!.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    fun processBluetoothMessage(btm: BluetoothMessage) {
        if (mStartedByBandLeader) {
            if (btm is ToggleStartStopMessage) {
                if (mSongView != null) {
                    if (btm.mTime >= 0)
                        mSongView!!.setSongTime(btm.mTime, true, false, true)
                    mSongView!!.startToggle(null, false, btm.mStartState)
                }
            } else if (btm is SetSongTimeMessage) {
                if (mSongView != null)
                    mSongView!!.setSongTime(btm)
            } else if (btm is PauseOnScrollStartMessage) {
                if (mSongView != null)
                    mSongView!!.pauseOnScrollStart()
            } else if (btm is QuitSongMessage)
                finish()
        }
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
            if (mSongView != null) {
                val eventTime = sensorEvent.timestamp
                activateOtherPageDown(eventTime)
            }
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
            when (msg.what) {
                EventHandler.BLUETOOTH_MESSAGE_RECEIVED -> mActivity.processBluetoothMessage(msg.obj as BluetoothMessage)
                EventHandler.MIDI_SET_SONG_POSITION -> mSongView?.setSongBeatPosition(msg.arg1, true)
                        ?: Log.d(BeatPrompterApplication.TAG, "MIDI song position pointer received by SongDisplay before view was created.")
                EventHandler.MIDI_START_SONG -> mSongView?.startSong(true, true)
                        ?: Log.d(BeatPrompterApplication.TAG, "MIDI start signal received by SongDisplay before view was created.")
                EventHandler.MIDI_CONTINUE_SONG -> mSongView?.startSong(true, false)
                        ?: Log.d(BeatPrompterApplication.TAG, "MIDI continue signal received by SongDisplay before view was created.")
                EventHandler.MIDI_STOP_SONG -> mSongView?.stopSong(true)
                        ?: Log.d(BeatPrompterApplication.TAG, "MIDI stop signal received by SongDisplay before view was created.")
                EventHandler.END_SONG -> {
                    mActivity.setResult(Activity.RESULT_OK)
                    mActivity.finish()
                }
                EventHandler.MIDI_LSB_BANK_SELECT -> MIDIController.mMidiBankLSBs[msg.arg1] = msg.arg2.toByte()
                EventHandler.MIDI_MSB_BANK_SELECT -> MIDIController.mMidiBankMSBs[msg.arg1] = msg.arg1.toByte()
            }
        }
    }

    companion object {
        private var mSongDisplayActive = false
        private lateinit var mSongDisplayInstance: SongDisplayActivity

        fun interruptCurrentSong(loadTask: SongLoadTask, songToInterruptWith: SongFile): SongInterruptResult {
            if (mSongDisplayActive) {
                val loadedSong = SongLoaderTask.currentSong
                        ?: return SongInterruptResult.NoSongToInterrupt

                // This should never happen, but we'll check just to be sure.
                // Trying to interrupt a song with itself is pointless!
                if (loadedSong.mSongFile.mID == songToInterruptWith.mID)
                    return SongInterruptResult.NoSongToInterrupt

                return if (mSongDisplayInstance.canYieldToExternalTrigger()) {
                    loadedSong.mCancelled = true
                    SongLoadTask.mSongLoadTaskOnResume = loadTask
                    EventHandler.sendEventToSongDisplay(EventHandler.END_SONG)
                    SongInterruptResult.CanInterrupt
                } else {
                    SongLoadTask.mSongLoadTaskOnResume = null
                    SongInterruptResult.CannotInterrupt
                }
            }
            return SongInterruptResult.NoSongToInterrupt
        }
    }
}
