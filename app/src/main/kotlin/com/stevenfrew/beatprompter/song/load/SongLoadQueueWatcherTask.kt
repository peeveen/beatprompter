package com.stevenfrew.beatprompter.song.load

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication.Companion.TAG_LOAD
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothManager
import com.stevenfrew.beatprompter.comm.bluetooth.message.ChooseSongMessage
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.ui.SongDisplayActivity

object SongLoadQueueWatcherTask : Task(true) {
    private val mSongsToLoad = mutableListOf<SongLoadJob>()
    private val nextSongToLoad: SongLoadJob?
        get() = synchronized(mSongsToLoad)
        {
            val first = mSongsToLoad.firstOrNull()
            if (first != null)
                mSongsToLoad.remove(first)
            first
        }
    val songCurrentlyLoading: Boolean
        get() = synchronized(mSongsToLoad)
        {
            mSongsToLoad.any { synchronized(it) { it.mLoading } }
        }

    override fun doWork() {
        val songToLoad = nextSongToLoad
        if (songToLoad != null) {
            Log.d(TAG_LOAD, "Found a song to load: ${songToLoad.mSongLoadInfo.mSongFile.mTitle}")
            synchronized(songToLoad)
            {
                songToLoad.startLoading()
            }
        } else
            Thread.sleep(250)
    }

    override fun stop() {
        stopCurrentLoads()
        super.stop()
    }

    private fun stopCurrentLoads() {
        synchronized(mSongsToLoad)
        {
            mSongsToLoad.forEach {
                if (synchronized(it) { it.mLoading })
                    Log.d(TAG_LOAD, "Cancelling started load: ${it.mSongLoadInfo.mSongFile.mTitle}")
                else
                    Log.d(TAG_LOAD, "Removing an unstarted load from the queue: ${it.mSongLoadInfo.mSongFile.mTitle}")
                it.stopLoading()
            }
            mSongsToLoad.clear()
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
        when (interruptResult) {
            SongInterruptResult.NoSongToInterrupt -> {
                // Create a bluetooth song-selection message to broadcast to other listeners.
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
                BluetoothManager.mBluetoothOutQueue.put(csm)

                synchronized(mSongsToLoad)
                {
                    Log.d(TAG_LOAD, "Adding a song to the load queue: ${loadJob.mSongLoadInfo.mSongFile.mTitle}")
                    mSongsToLoad.add(loadJob)
                }
            }
            SongInterruptResult.CannotInterrupt -> loadJob.stopLoading()
            SongInterruptResult.CanInterrupt -> {
            }
        }
    }
}