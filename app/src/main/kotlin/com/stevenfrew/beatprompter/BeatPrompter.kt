package com.stevenfrew.beatprompter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.preference.PreferenceManager
import com.stevenfrew.beatprompter.comm.ConnectionNotificationTask
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth
import com.stevenfrew.beatprompter.comm.midi.ClockSignalGeneratorTask
import com.stevenfrew.beatprompter.comm.midi.Midi
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

      override val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(applicationContext)

      override val privatePreferences: SharedPreferences
        get() = applicationContext.getSharedPreferences(SHARED_PREFERENCES_ID, Context.MODE_PRIVATE)

      override val assetManager: AssetManager
        get() = applicationContext.assets
    }
    applyPreferenceDefaults()
    AppCompatDelegate.setDefaultNightMode(if (Preferences.darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
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
    private val debugMessages = mutableListOf<String>()

    private val songLoaderTaskThread = Thread(SongLoadQueueWatcherTask)
    private val connectionNotificationTaskThread = Thread(ConnectionNotificationTask)
    val midiClockOutTaskThread =
      Thread(ClockSignalGeneratorTask).also { it.priority = Thread.MAX_PRIORITY }

    fun addDebugMessage(message: String) = debugMessages.add(message)
    val debugLog: String
      get() = debugMessages.takeLast(100).joinToString("\n")
  }
}
