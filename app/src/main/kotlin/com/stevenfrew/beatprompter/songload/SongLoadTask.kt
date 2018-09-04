package com.stevenfrew.beatprompter.songload

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.AsyncTask
import android.os.Message
import android.util.Log
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.bluetooth.BluetoothManager
import com.stevenfrew.beatprompter.bluetooth.ChooseSongMessage
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.SongFile
import java.util.concurrent.Semaphore

/**
 * This task does not actually load the song. It delegates the work to the main loader task (SongLoaderTask)
 * the SongParser class internally to load the song.
 * This task deals with the progress dialog UI side of things, and caters for situations where some
 * external event (MIDI, Bluetooth, double-tap) triggers the loading of a song either while a song is
 * currently active, or while a song is already being loaded.
 */
class SongLoadTask(selectedSong: SongFile, track: AudioFile?, scrollMode: ScrollingMode, nextSongName: String, startedByBandLeader: Boolean, startedByMidiTrigger: Boolean, nativeSettings: SongDisplaySettings, sourceSettings: SongDisplaySettings, private val mRegistered: Boolean) : AsyncTask<String, Int, Boolean>() {

    private var mCancelled = false
    private val mTaskEndSemaphore = Semaphore(0)
    private var mProgressTitle = ""
    private val mCancelEvent = SongLoadCancelEvent()
    private val mSongLoadInfo: SongLoadInfo = SongLoadInfo(selectedSong, track, scrollMode, nextSongName, startedByBandLeader, startedByMidiTrigger, nativeSettings, sourceSettings)
    private var mProgressDialog: ProgressDialog? = null
    private val mSongLoadTaskEventHandler: SongLoadTaskEventHandler

    init {
        mSongLoadTaskEventHandler = SongLoadTaskEventHandler(this)
    }

    override fun doInBackground(vararg paramParams: String): Boolean? {
        try {
            // The only thing that this "task" does here is attempt to acquire the
            // semaphore. The semaphore is created initially with zero permits, so
            // this will fail/wait until the semaphore is released, which occurs
            // when the handler receives a "completed" or "cancelled" message from
            // the SongLoaderTask.
            mTaskEndSemaphore.acquire()
        } catch (ignored: InterruptedException) {
        }

        return true
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        if (values.size > 1) {
            mProgressDialog!!.apply {
                setMessage(mProgressTitle + mSongLoadInfo.mSongFile.mTitle)
                max = values[1]!!
                progress = values[0]!!
            }
        }
    }

    override fun onPostExecute(b: Boolean?) {
        Log.d(AUTOLOAD_TAG, "In load task PostExecute.")
        super.onPostExecute(b)
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
        if (mCancelled)
            Log.d(AUTOLOAD_TAG, "Song load was cancelled.")
        else
            Log.d(AUTOLOAD_TAG, "Song loaded successfully.")
        Log.d(AUTOLOAD_TAG, "Song loaded successfully.")
        synchronized(mSongLoadSyncObject) {
            mSongLoadTask = null
        }
    }

    override fun onCancelled() {
        super.onCancelled()
    }

    override fun onCancelled(result: Boolean?) {
        super.onCancelled(result)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        mProgressDialog = ProgressDialog(SongList.mSongListInstance).apply {
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setMessage(mSongLoadInfo.mSongFile.mTitle)
            max = mSongLoadInfo.mSongFile.mLines
            isIndeterminate = false
            setCancelable(false)
            setButton(DialogInterface.BUTTON_NEGATIVE, Resources.getSystem().getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                mCancelled = true
                mCancelEvent.set()
            }
            show()
        }
    }

    /**
     * This is the entry point for kicking off the loading of a song file.
     */
    private fun loadSong() {
        // If the song-display activity is currently active, then try to interrupt
        // the current song with this one. If not possible, don't bother.
        val interruptResult = SongDisplayActivity.interruptCurrentSong(this, mSongLoadInfo.mSongFile)
        // A result of CannotInterrupt means that the current song refuses to stop. In which case, we can't load.
        // A result of CanInterrupt means that the current song has been instructed to end and, once it has, it will load the new one.
        // A result of NoSongToInterrupt, however, means full steam ahead.
        if (interruptResult === SongInterruptResult.NoSongToInterrupt) {

            // Create a bluetooth song-selection message to broadcast to other listeners.
            val csm = ChooseSongMessage(mSongLoadInfo.mSongFile.mNormalizedTitle,
                    mSongLoadInfo.mSongFile.mNormalizedArtist,
                    mSongLoadInfo.mTrack?.mName?:"",
                    mSongLoadInfo.mNativeDisplaySettings.mOrientation,
                    mSongLoadInfo.mSongLoadMode === ScrollingMode.Beat,
                    mSongLoadInfo.mSongLoadMode === ScrollingMode.Smooth,
                    mSongLoadInfo.mNativeDisplaySettings.mMinFontSize,
                    mSongLoadInfo.mNativeDisplaySettings.mMaxFontSize,
                    mSongLoadInfo.mNativeDisplaySettings.mScreenSize)
            BluetoothManager.broadcastMessageToClients(csm)

            // Kick off the loading of the new song.
            BeatPrompterApplication.loadSong(mSongLoadInfo, mSongLoadTaskEventHandler, mCancelEvent, mRegistered)
            this.execute()
        }
    }

    class SongLoadTaskEventHandler internal constructor(private var mSongLoadTask: SongLoadTask) : EventHandler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                EventHandler.SONG_LOAD_COMPLETED -> {
                    mSongLoadTask.mTaskEndSemaphore.release()
                    EventHandler.sendEventToSongList(EventHandler.SONG_LOAD_COMPLETED)
                }
                EventHandler.SONG_LOAD_CANCELLED -> {
                    mSongLoadTask.mCancelled = true
                    mSongLoadTask.mTaskEndSemaphore.release()
                }
                EventHandler.SONG_LOAD_LINE_PROCESSED -> {
                    mSongLoadTask.mProgressTitle = BeatPrompterApplication.getResourceString(R.string.processingSong)
                    mSongLoadTask.publishProgress(msg.arg1, msg.arg2)
                }
                EventHandler.SONG_LOAD_FAILED -> {
                    mSongLoadTask.mTaskEndSemaphore.release()
                    EventHandler.sendEventToSongList(EventHandler.SONG_LOAD_FAILED, msg.obj)
                }
            }
        }
    }

    companion object {
        private const val AUTOLOAD_TAG = "autoload"
        private val mSongLoadSyncObject = Any()
        private var mSongLoadTask: SongLoadTask? = null
        var mSongLoadTaskOnResume: SongLoadTask? = null

        fun loadSong(loadTask: SongLoadTask) {
            synchronized(mSongLoadSyncObject) {
                mSongLoadTask = loadTask
                // This was previously outside of this sync block.
                mSongLoadTask!!.loadSong()
            }
        }

        fun songCurrentlyLoading(): Boolean {
            synchronized(mSongLoadSyncObject) {
                return mSongLoadTask != null
            }
        }

        fun onResume() {
            if (mSongLoadTaskOnResume != null) {
                val resumeTask = mSongLoadTaskOnResume
                mSongLoadTaskOnResume = null
                SongLoadTask.loadSong(resumeTask!!)
            }
        }
    }
}
