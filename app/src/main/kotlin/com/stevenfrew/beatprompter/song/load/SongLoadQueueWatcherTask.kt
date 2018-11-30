package com.stevenfrew.beatprompter.song.load

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.BeatPrompterApplication.Companion.TAG_LOAD
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothManager
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothMode
import com.stevenfrew.beatprompter.comm.bluetooth.message.ChooseSongMessage
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.ui.SongDisplayActivity

object SongLoadQueueWatcherTask : Task(true) {
    private val mSongLoadLock = Any()
    private var mLoadingSong: SongLoadJob? = null
    private var mSongToLoad: SongLoadJob? = null
    private var mSongToLoadOnResume: SongLoadJob? = null
    private val nextSongToLoad: SongLoadJob?
        get() = synchronized(mSongLoadLock)
        {
            val stl = mSongToLoad
            mSongToLoad = null
            stl
        }
    val hasASongToLoad: Boolean
        get() = synchronized(mSongLoadLock)
        {
            mSongToLoad != null || mSongToLoadOnResume != null
        }
    val isLoadingASong: Boolean
        get() = synchronized(mSongLoadLock)
        {
            mLoadingSong != null
        }

    override fun doWork() {
        synchronized(mSongLoadLock)
        {
            val songToLoad = nextSongToLoad
            if (songToLoad != null) {
                mLoadingSong = songToLoad
                Log.d(TAG_LOAD, "Found a song to load: ${songToLoad.mSongLoadInfo.mSongFile.mTitle}")
                synchronized(songToLoad)
                {
                    songToLoad.startLoading()
                }
                return
            }
        }
        Thread.sleep(250)
    }

    fun onSongLoadFinished() {
        synchronized(mSongLoadLock)
        {
            mLoadingSong = null
        }
    }

    override fun stop() {
        stopCurrentLoads()
        super.stop()
    }

    private fun stopCurrentLoads() {
        synchronized(mSongLoadLock)
        {
            if (mSongToLoadOnResume != null) {
                Log.d(TAG_LOAD, "Removing an unstarted load-on-resume from the queue: ${mSongToLoadOnResume!!.mSongLoadInfo.mSongFile.mTitle}")
                mSongToLoadOnResume = null
            }
            if (mSongToLoad != null) {
                Log.d(TAG_LOAD, "Removing an unstarted load from the queue: ${mSongToLoad!!.mSongLoadInfo.mSongFile.mTitle}")
                mSongToLoad = null
            }
            if (mLoadingSong != null) {
                Log.d(TAG_LOAD, "Cancelling started load: ${mLoadingSong!!.mSongLoadInfo.mSongFile.mTitle}")
                mLoadingSong!!.stopLoading()
                mLoadingSong = null
            }
        }
    }

    fun loadSong(loadJob: SongLoadJob) {
        stopCurrentLoads()

        // If the song-display activity is currently active, then try to interrupt
        // the current song with this one. If not possible, don't bother.
        val interruptResult = SongDisplayActivity.interruptCurrentSong(loadJob)
        // A result of CannotInterrupt means that the current song refuses to stop. In which case, we can't load.
        // A result of CanInterrupt means that the current song has been instructed to end and, once it has, it will load the new one.
        // A result of NoSongToInterrupt, however, means full steam ahead.
        if (interruptResult != SongInterruptResult.CannotInterrupt) {
            // Create a bluetooth song-selection message to broadcast to other listeners.
            if (BluetoothManager.bluetoothMode == BluetoothMode.Server) {
                Log.d(BeatPrompterApplication.TAG_LOAD, "Sending ChooseSongMessage for \"${loadJob.mSongLoadInfo.mSongFile.mNormalizedTitle}\"")
                val csm = ChooseSongMessage(SongChoiceInfo(loadJob.mSongLoadInfo.mSongFile.mNormalizedTitle,
                        loadJob.mSongLoadInfo.mSongFile.mNormalizedArtist,
                        loadJob.mSongLoadInfo.mTrack?.mName ?: "",
                        loadJob.mSongLoadInfo.mNativeDisplaySettings.mOrientation,
                        loadJob.mSongLoadInfo.mSongLoadMode === ScrollingMode.Beat,
                        loadJob.mSongLoadInfo.mSongLoadMode === ScrollingMode.Smooth,
                        loadJob.mSongLoadInfo.mNativeDisplaySettings.mMinFontSize,
                        loadJob.mSongLoadInfo.mNativeDisplaySettings.mMaxFontSize,
                        loadJob.mSongLoadInfo.mNativeDisplaySettings.mScreenSize,
                        loadJob.mSongLoadInfo.mNoAudio))
                BluetoothManager.mBluetoothOutQueue.putMessage(csm)
            }
        }
        when (interruptResult) {
            SongInterruptResult.NoSongToInterrupt -> {
                synchronized(mSongLoadLock)
                {
                    Log.d(TAG_LOAD, "Adding a song to the load queue: ${loadJob.mSongLoadInfo.mSongFile.mTitle}")
                    mSongToLoadOnResume = null
                    mSongToLoad = loadJob
                }
            }
            SongInterruptResult.CannotInterrupt -> loadJob.stopLoading()
            SongInterruptResult.CanInterrupt -> {
                Log.d(TAG_LOAD, "CanInterrupt ... song will load when activity finishes.")
            }
        }
    }

    fun onResume() {
        synchronized(mSongLoadLock)
        {
            if (mSongToLoadOnResume != null) {
                val loadJob = mSongToLoadOnResume!!
                mSongToLoadOnResume = null
                SongLoadQueueWatcherTask.loadSong(loadJob)
            }
        }
    }

    fun setSongToLoadOnResume(songToLoadOnResume: SongLoadJob?) {
        synchronized(mSongLoadLock)
        {
            mSongToLoadOnResume = songToLoadOnResume
        }
    }
}