package com.stevenfrew.beatprompter

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import com.stevenfrew.beatprompter.comm.ConnectionNotificationTask
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth
import com.stevenfrew.beatprompter.comm.midi.ClockSignalGeneratorTask
import com.stevenfrew.beatprompter.comm.midi.Midi
import com.stevenfrew.beatprompter.preferences.AndroidPreferences
import com.stevenfrew.beatprompter.preferences.Preferences
import com.stevenfrew.beatprompter.song.load.SongLoadQueueWatcherTask
import com.stevenfrew.beatprompter.util.AndroidUtils
import com.stevenfrew.beatprompter.util.ApplicationContextResources
import com.stevenfrew.beatprompter.util.GlobalAppResources
import com.stevenfrew.beatprompter.util.PlatformUtils

class BeatPrompter : Application() {
	override fun attachBaseContext(base: Context) {
		super.attachBaseContext(base)
		MultiDex.install(this)
	}

	override fun onCreate() {
		super.onCreate()
		platformUtils = AndroidUtils(resources)
		appResources = ApplicationContextResources(resources)
		preferences = AndroidPreferences(appResources, applicationContext)

		AppCompatDelegate.setDefaultNightMode(if (preferences.darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
		Midi.initialize(applicationContext)
		Bluetooth.initialize(applicationContext)
		songLoaderTaskThread.start()
		midiClockOutTaskThread.start()
		connectionNotificationTaskThread.start()
		Task.resumeTask(SongLoadQueueWatcherTask, songLoaderTaskThread)
	}

	companion object {
		const val APP_NAME = "BeatPrompter"
		lateinit var appResources: GlobalAppResources
		lateinit var preferences: Preferences
		lateinit var platformUtils: PlatformUtils
		private val debugMessages = mutableListOf<String>()

		private val songLoaderTaskThread = Thread(SongLoadQueueWatcherTask)
		private val connectionNotificationTaskThread = Thread(ConnectionNotificationTask)
		val midiClockOutTaskThread =
			Thread(ClockSignalGeneratorTask).also { it.priority = Thread.MAX_PRIORITY }

		fun addDebugMessage(message: String) {
			if (BuildConfig.DEBUG) debugMessages.add(message)
		}

		val debugLog: String
			get() = debugMessages.takeLast(100).joinToString("\n")
	}
}
