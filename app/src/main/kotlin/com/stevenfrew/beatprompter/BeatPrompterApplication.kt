package com.stevenfrew.beatprompter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.os.Handler
import android.preference.PreferenceManager
import android.support.multidex.MultiDex
import com.stevenfrew.beatprompter.bluetooth.BluetoothManager
import com.stevenfrew.beatprompter.event.CancelEvent
import com.stevenfrew.beatprompter.midi.MIDIController
import com.stevenfrew.beatprompter.songload.SongLoadInfo
import com.stevenfrew.beatprompter.songload.SongLoaderTask

class BeatPrompterApplication : Application() {
    private val mSongLoaderTaskThread = Thread(mSongLoaderTask)

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
        Task.resumeTask(mSongLoaderTask)
    }

    override fun onTerminate() {
        Task.stopTask(mSongLoaderTask, mSongLoaderTaskThread)
        BluetoothManager.shutdown(this)
        MIDIController.shutdown(this)
        super.onTerminate()
    }

    companion object {
        const val TAG = "beatprompter"
        const val APP_NAME = "BeatPrompter"
        private var mApp: Application? = null
        private const val SHARED_PREFERENCES_ID = "beatPrompterSharedPreferences"
        private val mSongLoaderTask = SongLoaderTask()

        fun getResourceString(resID: Int): String {
            return mApp!!.getString(resID)
        }

        fun getResourceString(resID: Int, vararg args: Any): String {
            return mApp!!.getString(resID, *args)
        }

        val preferences: SharedPreferences
            get() = PreferenceManager.getDefaultSharedPreferences(mApp)

        val privatePreferences: SharedPreferences
            get() = mApp!!.getSharedPreferences(SHARED_PREFERENCES_ID, Context.MODE_PRIVATE)

        val assetManager: AssetManager
            get() = mApp!!.assets

        val context: Context
            get() = mApp!!.applicationContext

        fun loadSong(sli: SongLoadInfo, handler: Handler, cancelEvent: CancelEvent, registered: Boolean) {
            mSongLoaderTask.loadSong(sli, handler, cancelEvent, registered)
        }
    }
}