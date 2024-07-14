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
import android.os.Handler
import android.os.Message
import android.os.ParcelUuid
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothController
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothMode
import com.stevenfrew.beatprompter.comm.bluetooth.message.ChooseSongMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.ToggleStartStopMessage
import com.stevenfrew.beatprompter.comm.midi.ClockSignalGeneratorTask
import com.stevenfrew.beatprompter.comm.midi.MIDIController
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.load.SongChoiceInfo
import com.stevenfrew.beatprompter.song.load.SongInterruptResult
import com.stevenfrew.beatprompter.song.load.SongLoadJob
import com.stevenfrew.beatprompter.song.load.SongLoadQueueWatcherTask

class SongDisplayActivity
	: AppCompatActivity(),
	SensorEventListener {
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

	private val mMidiClockOutTask = ClockSignalGeneratorTask()
	private val mMidiClockOutTaskThread =
		Thread(mMidiClockOutTask).also { it.priority = Thread.MAX_PRIORITY }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		mSongDisplayInstance = this

		val sendMidiClockPref = Preferences.sendMIDIClock
		mScrollOnProximity = Preferences.proximityScroll
		mAnyOtherKeyPageDown = Preferences.anyOtherKeyPageDown

		setContentView(R.layout.activity_song_display)
		val potentiallyNullSongView: SongView? = findViewById(R.id.song_view)
		if (potentiallyNullSongView == null) {
			finish()
			System.gc()
			return
		}
		mSongView = potentiallyNullSongView

		val loadedSong = SongLoadJob.mLoadedSong
		if (loadedSong == null) {
			finish()
			System.gc()
			return
		}
		val song = loadedSong.mSong
		val loadID: ParcelUuid = intent.extras?.get("loadID") as ParcelUuid
		// For a song to load successfully, all three IDs must match:
		// 1) The ID passed in the startActivityForResult parcelable
		// 2) The ID of the current loadedSong in SongLoadJob
		// 3) The static ID in this class set by SongListActivity
		// If there is any mismatch, then there has been a cancellation caused by another
		// song being loaded very quickly. We need all this checking because anything can
		// happen in the split second between this SongDisplay activity being launched, and
		// us reaching this code here. If we don't finish() in the event of a mismatch, we
		// can end up with multiple SongDisplay activities running.
		if (song.mLoadID != loadID.uuid) {
			Logger.logLoader("*** Load ID Mismatch ***")
			Logger.logLoader { "Parcelable Load ID = ${loadID.uuid}" }
			Logger.logLoader { "SongLoadJob ID = ${song.mLoadID}" }
			finish()
			System.gc()
			return
		} else {
			Logger.logLoader { "Successful load ID match: ${song.mLoadID}" }
			if (Preferences.bluetoothMode == BluetoothMode.Server) {
				Logger.logLoader { "Sending ChooseSongMessage for \"${loadedSong.mLoadJob.mSongLoadInfo.mSongFile.mNormalizedTitle}\"" }
				val csm = ChooseSongMessage(
					SongChoiceInfo(
						loadedSong.mLoadJob.mSongLoadInfo.mSongFile.mNormalizedTitle,
						loadedSong.mLoadJob.mSongLoadInfo.mSongFile.mNormalizedArtist,
						loadedSong.mLoadJob.mSongLoadInfo.mVariation,
						loadedSong.mLoadJob.mSongLoadInfo.mNativeDisplaySettings.mOrientation,
						loadedSong.mLoadJob.mSongLoadInfo.mSongLoadMode === ScrollingMode.Beat,
						loadedSong.mLoadJob.mSongLoadInfo.mSongLoadMode === ScrollingMode.Smooth,
						loadedSong.mLoadJob.mSongLoadInfo.mNativeDisplaySettings.mMinFontSize,
						loadedSong.mLoadJob.mSongLoadInfo.mNativeDisplaySettings.mMaxFontSize,
						loadedSong.mLoadJob.mSongLoadInfo.mNativeDisplaySettings.mScreenSize,
						loadedSong.mLoadJob.mSongLoadInfo.mNoAudio
					)
				)
				BluetoothController.putMessage(csm)
			}
		}

		mStartedByBandLeader = song.mStartedByBandLeader
		val orientation = song.mDisplaySettings.mOrientation
		mOrientation = if (orientation == Configuration.ORIENTATION_LANDSCAPE)
			ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
		else
			ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
		requestedOrientation = mOrientation

		MIDIController.putMessages(song.mInitialMIDIMessages)

		window.setFlags(
			WindowManager.LayoutParams.FLAG_FULLSCREEN,
			WindowManager.LayoutParams.FLAG_FULLSCREEN
		)

		mSongDisplayEventHandler = SongDisplayEventHandler(this, mSongView)
		EventRouter.setSongDisplayEventHandler(mSongDisplayEventHandler)
		mSongView!!.init(this, song)

		if (sendMidiClockPref || song.mSendMIDIClock)
			mMidiClockOutTaskThread.start()

		try {
			mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
			if (mSensorManager != null) {
				mProximitySensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
			}
		} catch (e: Exception) {
			// Nae sensors.
		}

		System.gc()

		// Set up the user interaction to manually show or hide the system UI.
		mSongView!!.setOnClickListener { }
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
		if (!mSongDisplayActive)
			SongLoadJob.mLoadedSong = null

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
		// A foot on a keyboard can generate a lot of events!
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

	class SongDisplayEventHandler internal constructor(
		private val mActivity: SongDisplayActivity,
		private val mSongView: SongView?
	) : Handler() {
		override fun handleMessage(msg: Message) {
			if (mSongDisplayActive)
				when (msg.what) {
					Events.BLUETOOTH_PAUSE_ON_SCROLL_START -> mSongView?.pauseOnScrollStart()
					Events.BLUETOOTH_QUIT_SONG -> {
						Logger.logLoader("Quit song Bluetooth message received. Finishing activity.")
						val songInfo = msg.obj as Pair<*, *>
						val title = songInfo.first as String
						val artist = songInfo.second as String
						if (mSongView != null)
							if (mSongView.hasSong(title, artist))
								mActivity.finish()
					}

					Events.BLUETOOTH_SET_SONG_TIME -> mSongView?.setSongTime(
						msg.obj as Long,
						redraw = true,
						broadcast = false,
						setPixelPosition = true,
						recalculateManualPositions = true
					)

					Events.BLUETOOTH_TOGGLE_START_STOP -> mSongView?.processBluetoothToggleStartStopMessage(
						msg.obj as ToggleStartStopMessage.StartStopToggleInfo
					)

					Events.MIDI_SET_SONG_POSITION -> mSongView?.setSongBeatPosition(msg.arg1, true)
						?: Logger.log("MIDI song position pointer received by SongDisplay before view was created.")

					Events.MIDI_START_SONG -> mSongView?.startSong(midiInitiated = true, fromStart = true)
						?: Logger.log("MIDI start signal received by SongDisplay before view was created.")

					Events.MIDI_CONTINUE_SONG -> mSongView?.startSong(midiInitiated = true, fromStart = false)
						?: Logger.log("MIDI continue signal received by SongDisplay before view was created.")

					Events.MIDI_STOP_SONG -> mSongView?.stopSong(true)
						?: Logger.log("MIDI stop signal received by SongDisplay before view was created.")

					Events.END_SONG -> {
						mActivity.setResult(Activity.RESULT_OK)
						Logger.logLoader("End song message received. Finishing activity.")
						mActivity.finish()
					}
				}
		}
	}

	companion object {
		var mSongDisplayActive = false
		private lateinit var mSongDisplayInstance: SongDisplayActivity

		fun interruptCurrentSong(interruptJob: SongLoadJob): SongInterruptResult {
			if (mSongDisplayActive) {
				val loadedSong = SongLoadJob.mLoadedSong
					?: return SongInterruptResult.NoSongToInterrupt

				return when {
					// Trying to interrupt a song with itself is pointless!
					loadedSong.mSong.mSongFile.mID == interruptJob.mSongLoadInfo.mSongFile.mID -> SongInterruptResult.SongAlreadyLoaded
					mSongDisplayInstance.canYieldToExternalTrigger() -> {
						loadedSong.mSong.mCancelled = true
						SongLoadQueueWatcherTask.setSongToLoadOnResume(interruptJob)
						EventRouter.sendEventToSongDisplay(Events.END_SONG)
						SongInterruptResult.CanInterrupt
					}

					else -> {
						SongLoadQueueWatcherTask.setSongToLoadOnResume(null)
						SongInterruptResult.CannotInterrupt
					}
				}
			}
			return SongInterruptResult.NoSongToInterrupt
		}
	}
}
