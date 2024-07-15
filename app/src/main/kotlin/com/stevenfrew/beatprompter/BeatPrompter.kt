package com.stevenfrew.beatprompter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.preference.PreferenceManager
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothController
import com.stevenfrew.beatprompter.comm.midi.MidiController
import com.stevenfrew.beatprompter.song.load.SongLoadQueueWatcherTask

interface GlobalAppResources {
	fun getString(resID: Int): String
	fun getString(resID: Int, vararg args: Any): String
	val preferences: SharedPreferences
	val privatePreferences: SharedPreferences
	val assetManager: AssetManager
	val context: Context
}

class BeatPrompter : Application() {
	private val mSongLoaderTaskThread = Thread(SongLoadQueueWatcherTask)

	override fun attachBaseContext(base: Context) {
		super.attachBaseContext(base)
		MultiDex.install(this)
	}

	override fun onCreate() {
		super.onCreate()
		appResources = object : GlobalAppResources {
			override fun getString(resID: Int): String {
				return applicationContext.getString(resID)
			}

			override fun getString(resID: Int, vararg args: Any): String {
				return applicationContext.getString(resID, args)
			}

			override val preferences: SharedPreferences
				get() = PreferenceManager.getDefaultSharedPreferences(applicationContext)

			override val privatePreferences: SharedPreferences
				get() = applicationContext.getSharedPreferences(SHARED_PREFERENCES_ID, Context.MODE_PRIVATE)

			override val assetManager: AssetManager
				get() = applicationContext.assets

			override val context: Context
				get() = applicationContext
		}
		applyPreferenceDefaults()
		AppCompatDelegate.setDefaultNightMode(if (Preferences.darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
		MidiController.initialize(applicationContext)
		BluetoothController.initialise(applicationContext)
		mSongLoaderTaskThread.start()
		Task.resumeTask(SongLoadQueueWatcherTask)
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
	}
}
