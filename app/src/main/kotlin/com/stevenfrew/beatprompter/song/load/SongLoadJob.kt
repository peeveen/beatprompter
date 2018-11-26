package com.stevenfrew.beatprompter.song.load

import android.os.Message
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.BeatPrompterApplication.Companion.TAG_LOAD
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.SongParser
import com.stevenfrew.beatprompter.song.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Semaphore
import kotlin.coroutines.CoroutineContext

class SongLoadJob(val mSongLoadInfo:SongLoadInfo, private val mRegistered: Boolean): CoroutineScope {
    var mLoading=false
    private val mLoadingUI:SongLoadUITask = SongLoadUITask(this)
    private val mSemaphore=Semaphore(0)
    private val mHandler=SongLoadJobEventHandler(this)
    private val mCancelEvent=SongLoadCancelEvent(mSongLoadInfo.mSongFile.mTitle)
    private val mCoRoutineJob= Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + mCoRoutineJob

    init {
        mLoadingUI.execute()
    }

    fun startLoading()
    {
        synchronized(this)
        {
            mLoading = true
            launch {
                System.gc()
                try {
                    Log.d(TAG_LOAD, "Starting to load '${mSongLoadInfo.mSongFile.mTitle}'.")
                    val loadedSong = SongParser(mSongLoadInfo, mCancelEvent, mHandler, mRegistered).parse()
                    if (mCancelEvent.isCancelled)
                        throw SongLoadCancelledException()
                    Log.d(TAG_LOAD, "Song was loaded successfully.")
                    mLoadedSong=loadedSong
                    mHandler.obtainMessage(EventHandler.SONG_LOAD_COMPLETED, mSongLoadInfo.mLoadID).sendToTarget()
                } catch (e: SongLoadCancelledException) {
                    Log.d(TAG_LOAD, "Song load was cancelled.")
                    mHandler.obtainMessage(EventHandler.SONG_LOAD_CANCELLED).sendToTarget()
                } catch (e: Exception) {
                    Log.d(TAG_LOAD, "Song load failed.")
                    mHandler.obtainMessage(EventHandler.SONG_LOAD_FAILED, e.message).sendToTarget()
                }
                finally {
                    System.gc()
                }
            }
        }
    }

    fun stopLoading()
    {
        mCancelEvent.set()
        mLoadingUI.cancelLoad()
    }

    fun onCompletion()
    {
        mSemaphore.release()
    }

    fun waitForCompletion()
    {
        mSemaphore.acquire()
    }

    class SongLoadJobEventHandler internal constructor(private val mLoadJob:SongLoadJob) : EventHandler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                EventHandler.SONG_LOAD_COMPLETED -> {
                    mLoadJob.onCompletion()
                    EventHandler.sendEventToSongList(EventHandler.SONG_LOAD_COMPLETED,msg.obj)
                }
                EventHandler.SONG_LOAD_FAILED -> {
                    mLoadJob.onCompletion()
                    EventHandler.sendEventToSongList(EventHandler.SONG_LOAD_FAILED, msg.obj)
                }
                EventHandler.SONG_LOAD_CANCELLED -> mLoadJob.onCompletion()
                EventHandler.SONG_LOAD_LINE_PROCESSED -> mLoadJob.mLoadingUI.updateProgress(BeatPrompterApplication.getResourceString(R.string.processingSong,mLoadJob.mSongLoadInfo.mSongFile.mTitle),msg.arg1, msg.arg2)
            }
        }
    }

    companion object {
        var mLoadedSong:Song?=null
        var mSongLoadJobOnResume:SongLoadJob?=null

        fun onResume()
        {
            if(mSongLoadJobOnResume!=null)
            {
                val loadJob=mSongLoadJobOnResume!!
                mSongLoadJobOnResume=null
                SongLoadQueueWatcherTask.loadSong(loadJob)
            }
        }
    }
}