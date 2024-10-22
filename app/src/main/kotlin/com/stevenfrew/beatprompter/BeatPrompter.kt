package com.stevenfrew.beatprompter

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.preference.PreferenceManager
import com.stevenfrew.beatprompter.comm.ConnectionNotificationTask
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth
import com.stevenfrew.beatprompter.comm.midi.ClockSignalGeneratorTask
import com.stevenfrew.beatprompter.comm.midi.Midi
import com.stevenfrew.beatprompter.graphics.bitmaps.AndroidBitmapFactory
import com.stevenfrew.beatprompter.graphics.bitmaps.BitmapFactory
import com.stevenfrew.beatprompter.graphics.fonts.AndroidFontManager
import com.stevenfrew.beatprompter.graphics.fonts.FontManager
import com.stevenfrew.beatprompter.preferences.AndroidPreferences
import com.stevenfrew.beatprompter.preferences.Preferences
import com.stevenfrew.beatprompter.song.load.SongLoadQueueWatcherTask
import com.stevenfrew.beatprompter.util.GlobalAppResources

class BeatPrompter : Application() {
	override fun attachBaseContext(base: Context) {
		super.attachBaseContext(base)
		MultiDex.install(this)
	}

	override fun onCreate() {
		super.onCreate()
		appResources = object : GlobalAppResources {
			override fun getString(resID: Int): String = applicationContext.getString(resID)

			override fun getString(resID: Int, vararg args: Any): String =
				applicationContext.getString(resID, *args)

			override fun getStringSet(resID: Int): Set<String> =
				applicationContext.resources.getStringArray(resID).toSet()

			override val assetManager: AssetManager
				get() = applicationContext.assets
		}
		preferences = AndroidPreferences(
			appResources,
			PreferenceManager.getDefaultSharedPreferences(applicationContext),
			applicationContext.getSharedPreferences(SHARED_PREFERENCES_ID, MODE_PRIVATE)
		)

		val minimumFontSize = getString(R.string.fontSizeMin).toFloat()
		val maximumFontSize = getString(R.string.fontSizeMax).toFloat()
		fontManager =
			AndroidFontManager(minimumFontSize, maximumFontSize, resources.displayMetrics.density)
		bitmapFactory = AndroidBitmapFactory
		applyPreferenceDefaults()
		AppCompatDelegate.setDefaultNightMode(if (preferences.darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
		Midi.initialize(applicationContext)
		Bluetooth.initialize(applicationContext)
		songLoaderTaskThread.start()
		midiClockOutTaskThread.start()
		connectionNotificationTaskThread.start()
		Task.resumeTask(SongLoadQueueWatcherTask, songLoaderTaskThread)
	}

	private fun applyPreferenceDefaults() {
		PreferenceManager.setDefaultValues(applicationContext, R.xml.preferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.fontsizepreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.colorpreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.filepreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.midipreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.bluetoothpreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.permissionpreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.songdisplaypreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.audiopreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.songlistpreferences, true)
	}

	companion object {
		const val APP_NAME = "BeatPrompter"
		private const val SHARED_PREFERENCES_ID = "beatPrompterSharedPreferences"
		lateinit var appResources: GlobalAppResources
		lateinit var preferences: Preferences
		lateinit var fontManager: FontManager
		lateinit var bitmapFactory: BitmapFactory
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
