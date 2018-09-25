package com.stevenfrew.beatprompter.song.load

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.song.Song
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.cache.parse.SongParser

class SongLoaderTask : Task(true) {
    private var mLoadingJob: SongLoadJob? = null
    private val mLoadingJobSync = Any()

    override fun doWork() {
        val slj:SongLoadJob?=
        {
            synchronized(mLoadingJobSync) {
                val returnValue=mLoadingJob
                mLoadingJob = null
                returnValue
            }
        }.invoke()
        if (slj != null) {
            System.gc()
            try {
                val loadedSong = SongParser(slj.mSongLoadInfo,slj.mCancelEvent,slj.mHandler,slj.mRegistered).parse()
                if (slj.mCancelEvent.isCancelled)
                    throw SongLoadCancelledException()
                Log.d(BeatPrompterApplication.TAG,"Song was loaded successfully.")
                currentSong = loadedSong
                slj.mHandler.obtainMessage(EventHandler.SONG_LOAD_COMPLETED).sendToTarget()
            } catch (e: SongLoadCancelledException) {
                Log.d(BeatPrompterApplication.TAG,"Song load was cancelled.")
                slj.mHandler.obtainMessage(EventHandler.SONG_LOAD_CANCELLED).sendToTarget()
            } catch (e: Exception) {
                Log.d(BeatPrompterApplication.TAG,"Song load failed.")
                slj.mHandler.obtainMessage(EventHandler.SONG_LOAD_FAILED, e.message).sendToTarget()
            }

            System.gc()
        } else
            try {
                // Nothing to do, wait a bit
                Thread.sleep(250)
            } catch (ignored: InterruptedException) { }
    }

    fun loadSong(songLoadJob:SongLoadJob) {
        synchronized(mLoadingJobSync) {
            Log.d(BeatPrompterApplication.TAG,"In SongLoaderTask.loadSong() ...")
            mLoadingJob?.mCancelEvent?.set()
            mLoadingJob=songLoadJob
        }
    }

    companion object {
        private var mCurrentSong: Song? = null

        private val mCurrentSongSync = Any()

        var currentSong: Song?
            get() = synchronized(mCurrentSongSync) {
                return mCurrentSong
            }
            private set(song) = synchronized(mCurrentSongSync) {
                mCurrentSong = song
                System.gc()
            }
    }
}
