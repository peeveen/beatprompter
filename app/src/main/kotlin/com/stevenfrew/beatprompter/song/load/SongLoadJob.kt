package com.stevenfrew.beatprompter.song.load

import android.os.Handler
import android.os.Message
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.cache.parse.SongParser
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class SongLoadJob(val songLoadInfo: SongLoadInfo) : CoroutineScope {
	private val handler = SongLoadJobEventHandler()
	private val cancelEvent = SongLoadCancelEvent(songLoadInfo.songFile.title)
	private val coRoutineJob = Job()

	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Default + coRoutineJob

	fun startLoading() =
		synchronized(this)
		{
			val thisSongLoadJob = this
			launch {
				System.gc()
				try {
					Logger.logLoader({ "Starting to load '${songLoadInfo.songFile.title}'." })
					val loadedSong =
						SongParser(songLoadInfo, Cache.supportFileResolver, cancelEvent, handler).parse()
					if (cancelEvent.isCancelled)
						throw SongLoadCancelledException()
					Logger.logLoader("Song was loaded successfully.")
					SongLoadQueueWatcherTask.onSongLoadFinished()
					mLoadedSong = LoadedSong(loadedSong, thisSongLoadJob)
					handler.obtainMessage(Events.SONG_LOAD_COMPLETED, songLoadInfo.loadId).sendToTarget()
				} catch (e: SongLoadCancelledException) {
					Logger.logLoader("Song load was cancelled.")
					SongLoadQueueWatcherTask.onSongLoadFinished()
					handler.obtainMessage(Events.SONG_LOAD_CANCELLED).sendToTarget()
				} catch (e: Exception) {
					Logger.logLoader("Song load failed.")
					SongLoadQueueWatcherTask.onSongLoadFinished()
					handler.obtainMessage(Events.SONG_LOAD_FAILED, e.message).sendToTarget()
				} finally {
					System.gc()
				}
			}
		}

	fun stopLoading() = cancelEvent.set()

	class SongLoadJobEventHandler : Handler() {
		override fun handleMessage(msg: Message) {
			when (msg.what) {
				Events.SONG_LOAD_CANCELLED ->
					EventRouter.sendEventToSongList(msg.what)

				Events.SONG_LOAD_FAILED, Events.SONG_LOAD_COMPLETED ->
					EventRouter.sendEventToSongList(msg.what, msg.obj)

				Events.SONG_LOAD_LINE_PROCESSED ->
					EventRouter.sendEventToSongList(Events.SONG_LOAD_LINE_PROCESSED, msg.arg1, msg.arg2)
			}
		}
	}

	companion object {
		var mLoadedSong: LoadedSong? = null
	}
}