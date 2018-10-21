package com.stevenfrew.beatprompter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.preference.PreferenceManager
import android.support.multidex.MultiDex
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothManager
import com.stevenfrew.beatprompter.comm.midi.MIDIController
import com.stevenfrew.beatprompter.song.load.SongLoadQueueWatcherTask

class BeatPrompterApplication : Application() {
    private val mSongLoaderTaskThread = Thread(SongLoadQueueWatcherTask)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        mApp = this
        MIDIController.initialise(this)
        BluetoothManager.initialise(this)
        mSongLoaderTaskThread.start()
        Task.resumeTask(SongLoadQueueWatcherTask)
    }

    override fun onTerminate() {
        Task.stopTask(SongLoadQueueWatcherTask, mSongLoaderTaskThread)
        BluetoothManager.shutdown(this)
        MIDIController.shutdown(this)
        super.onTerminate()
    }

    companion object {
        const val TAG = "beatprompter"
        const val TAG_LOAD = "beatprompter_load"
        const val TAG_COMMS = "beatprompter_comms"
        const val APP_NAME = "BeatPrompter"
        private lateinit var mApp: Application
        private const val SHARED_PREFERENCES_ID = "beatPrompterSharedPreferences"

        fun getResourceString(resID: Int): String {
            return mApp.getString(resID)
        }

        fun getResourceString(resID: Int, vararg args: Any): String {
            return mApp.getString(resID, *args)
        }

        val preferences: SharedPreferences
            get() = PreferenceManager.getDefaultSharedPreferences(mApp)

        val privatePreferences: SharedPreferences
            get() = mApp.getSharedPreferences(SHARED_PREFERENCES_ID, Context.MODE_PRIVATE)

        val assetManager: AssetManager
            get() = mApp.assets

        val context: Context
            get() = mApp.applicationContext
    }
}
