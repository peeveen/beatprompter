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
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothMode
import com.stevenfrew.beatprompter.comm.bluetooth.message.ChooseSongMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.ToggleStartStopMessage
import com.stevenfrew.beatprompter.comm.midi.ClockSignalGeneratorTask
import com.stevenfrew.beatprompter.comm.midi.Midi
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.load.SongChoiceInfo
import com.stevenfrew.beatprompter.song.load.SongInterruptResult
import com.stevenfrew.beatprompter.song.load.SongLoadJob
import com.stevenfrew.beatprompter.song.load.SongLoadQueueWatcherTask

class SongDisplayActivity
	: AppCompatActivity(),
	SensorEventListener {
	private var songView: SongView? = null
	private var wasStartedByBandLeader = false
	private var preferredOrientation: Int = 0
	private var orientation: Int = 0

	private var sensorManager: SensorManager? = null
	private var proximitySensor: Sensor? = null
	private var scrollOnProximity: Boolean = false

	private var lastOtherPageDownEvent: Long = 0
	private var anyOtherKeyPageDown = false

	private lateinit var songDisplayEventHandler: SongDisplayEventHandler

	private val midiClockOutTask = ClockSignalGeneratorTask()
	private val midiClockOutTaskThread =
		Thread(midiClockOutTask).also { it.priority = Thread.MAX_PRIORITY }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		songDisplayInstance = this

		val sendMidiClockPref = Preferences.sendMIDIClock
		scrollOnProximity = Preferences.proximityScroll
		anyOtherKeyPageDown = Preferences.anyOtherKeyPageDown

		setContentView(R.layout.activity_song_display)
		val potentiallyNullSongView: SongView? = findViewById(R.id.song_view)
		if (potentiallyNullSongView == null) {
			finish()
			System.gc()
			return
		}
		songView = potentiallyNullSongView

		val loadedSong = SongLoadJob.mLoadedSong
		if (loadedSong == null) {
			finish()
			System.gc()
			return
		}
		val song = loadedSong.song
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
		if (song.loadId != loadID.uuid) {
			Logger.logLoader("*** Load ID Mismatch ***")
			Logger.logLoader { "Parcelable Load ID = ${loadID.uuid}" }
			Logger.logLoader { "SongLoadJob ID = ${song.loadId}" }
			finish()
			System.gc()
			return
		} else {
			Logger.logLoader { "Successful load ID match: ${song.loadId}" }
			if (Preferences.bluetoothMode == BluetoothMode.Server) {
				Logger.logLoader { "Sending ChooseSongMessage for \"${loadedSong.loadJob.songLoadInfo.songFile.normalizedTitle}\"" }
				val csm = ChooseSongMessage(
					SongChoiceInfo(
						loadedSong.loadJob.songLoadInfo.songFile.normalizedTitle,
						loadedSong.loadJob.songLoadInfo.songFile.normalizedArtist,
						loadedSong.loadJob.songLoadInfo.variation,
						loadedSong.loadJob.songLoadInfo.nativeDisplaySettings.orientation,
						loadedSong.loadJob.songLoadInfo.songLoadMode === ScrollingMode.Beat,
						loadedSong.loadJob.songLoadInfo.songLoadMode === ScrollingMode.Smooth,
						loadedSong.loadJob.songLoadInfo.nativeDisplaySettings.minimumFontSize,
						loadedSong.loadJob.songLoadInfo.nativeDisplaySettings.maximumFontSize,
						loadedSong.loadJob.songLoadInfo.nativeDisplaySettings.screenSize,
						loadedSong.loadJob.songLoadInfo.noAudio,
						loadedSong.loadJob.songLoadInfo.audioLatency
					)
				)
				Bluetooth.putMessage(csm)
			}
		}

		wasStartedByBandLeader = song.wasStartedByBandLeader
		val orientation = song.displaySettings.orientation
		this.orientation = if (orientation == Configuration.ORIENTATION_LANDSCAPE)
			ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
		else
			ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
		requestedOrientation = this.orientation

		Midi.putMessages(song.initialMidiMessages)

		window.setFlags(
			WindowManager.LayoutParams.FLAG_FULLSCREEN,
			WindowManager.LayoutParams.FLAG_FULLSCREEN
		)

		songDisplayEventHandler = SongDisplayEventHandler(this, songView)
		EventRouter.setSongDisplayEventHandler(songDisplayEventHandler)
		songView!!.init(this, song)

		if (sendMidiClockPref || song.sendMidiClock)
			midiClockOutTaskThread.start()

		try {
			sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
			if (sensorManager != null) {
				proximitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
			}
		} catch (e: Exception) {
			// Nae sensors.
		}

		System.gc()

		// Set up the user interaction to manually show or hide the system UI.
		songView!!.setOnClickListener { }
	}

	fun canYieldToExternalTrigger(): Boolean =
		songView == null || songView!!.canYieldToExternalTrigger()

	override fun onPause() {
		songDisplayActive = false
		super.onPause()
		Task.pauseTask(midiClockOutTask, midiClockOutTaskThread)
		if (songView != null)
			songView!!.stop(false)
		if (sensorManager != null && proximitySensor != null)
			sensorManager!!.unregisterListener(this)
	}

	override fun onDestroy() {
		requestedOrientation = preferredOrientation
		super.onDestroy()
		if (!songDisplayActive)
			SongLoadJob.mLoadedSong = null

		Task.stopTask(midiClockOutTask, midiClockOutTaskThread)

		if (songView != null) {
			songView!!.stop(true)
			songView = null
		}
	}

	override fun onStop() {
		super.onStop()
		if (songView != null)
			songView!!.stop(false)
	}

	override fun onResume() {
		songDisplayActive = true
		super.onResume()
		requestedOrientation = orientation
		Task.resumeTask(midiClockOutTask)
		requestedOrientation = orientation
		if (sensorManager != null && proximitySensor != null)
			sensorManager!!.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST)
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
		when (keyCode) {
			KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_PAGE_DOWN -> {
				if (songView != null)
					songView!!.onPageDownKeyPressed()
				return true
			}

			KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_PAGE_UP -> {
				if (songView != null)
					songView!!.onPageUpKeyPressed()
				return true
			}

			KeyEvent.KEYCODE_DPAD_DOWN -> {
				if (songView != null)
					songView!!.onLineDownKeyPressed()
				return true
			}

			KeyEvent.KEYCODE_DPAD_UP -> {
				if (songView != null)
					songView!!.onLineUpKeyPressed()
				return true
			}

			KeyEvent.KEYCODE_DPAD_LEFT -> {
				if (songView != null)
					songView!!.onLeftKeyPressed()
				return true
			}

			KeyEvent.KEYCODE_DPAD_RIGHT -> {
				if (songView != null)
					songView!!.onRightKeyPressed()
				return true
			}

			else -> return if (anyOtherKeyPageDown) {
				activateOtherPageDown(System.nanoTime())
				true
			} else
				super.onKeyUp(keyCode, event)
		}
	}

	override fun onSensorChanged(sensorEvent: SensorEvent) {
		if (scrollOnProximity && songView != null)
			activateOtherPageDown(sensorEvent.timestamp)
	}

	private fun activateOtherPageDown(eventTime: Long) {
		// Don't allow two of these events to happen within the same second.
		// A foot on a keyboard can generate a lot of events!
		if (eventTime - lastOtherPageDownEvent > 1000000000) {
			lastOtherPageDownEvent = eventTime
			songView!!.onOtherPageDownActivated()
		}
	}

	override fun onAccuracyChanged(sensor: Sensor, i: Int) {
		// Don't care.
	}

	fun onSongBeat(bpm: Double) {
		if (bpm != 0.0)
			midiClockOutTask.setBPM(bpm)
	}

	fun onSongStop() = Task.stopTask(midiClockOutTask, midiClockOutTaskThread)

	class SongDisplayEventHandler internal constructor(
		private val activity: SongDisplayActivity,
		private val songView: SongView?
	) : Handler() {
		override fun handleMessage(msg: Message) {
			if (songDisplayActive)
				when (msg.what) {
					Events.BLUETOOTH_PAUSE_ON_SCROLL_START -> songView?.pauseOnScrollStart()
					Events.BLUETOOTH_QUIT_SONG -> {
						Logger.logLoader("Quit song Bluetooth message received. Finishing activity.")
						val songInfo = msg.obj as Pair<*, *>
						val title = songInfo.first as String
						val artist = songInfo.second as String
						if (songView != null)
							if (songView.hasSong(title, artist))
								activity.finish()
					}

					Events.BLUETOOTH_SET_SONG_TIME -> songView?.setSongTime(
						msg.obj as Long,
						redraw = true,
						broadcast = false,
						setPixelPosition = true,
						recalculateManualPositions = true
					)

					Events.BLUETOOTH_TOGGLE_START_STOP -> songView?.processBluetoothToggleStartStopMessage(
						msg.obj as ToggleStartStopMessage.StartStopToggleInfo
					)

					Events.MIDI_SET_SONG_POSITION -> songView?.setSongBeatPosition(msg.arg1, true)
						?: Logger.log("MIDI song position pointer received by SongDisplay before view was created.")

					Events.MIDI_START_SONG -> songView?.startSong(midiInitiated = true, fromStart = true)
						?: Logger.log("MIDI start signal received by SongDisplay before view was created.")

					Events.MIDI_CONTINUE_SONG -> songView?.startSong(midiInitiated = true, fromStart = false)
						?: Logger.log("MIDI continue signal received by SongDisplay before view was created.")

					Events.MIDI_STOP_SONG -> songView?.stopSong(true)
						?: Logger.log("MIDI stop signal received by SongDisplay before view was created.")

					Events.END_SONG -> {
						activity.setResult(Activity.RESULT_OK)
						Logger.logLoader("End song message received. Finishing activity.")
						activity.finish()
					}
				}
		}
	}

	companion object {
		var songDisplayActive = false
		private lateinit var songDisplayInstance: SongDisplayActivity

		fun interruptCurrentSong(interruptJob: SongLoadJob): SongInterruptResult {
			if (songDisplayActive) {
				val loadedSong = SongLoadJob.mLoadedSong
					?: return SongInterruptResult.NoSongToInterrupt

				return when {
					// Trying to interrupt a song with itself is pointless!
					loadedSong.song.songFile.id == interruptJob.songLoadInfo.songFile.id -> SongInterruptResult.SongAlreadyLoaded
					songDisplayInstance.canYieldToExternalTrigger() -> {
						loadedSong.song.cancelled = true
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
