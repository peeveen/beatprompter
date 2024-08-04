package com.stevenfrew.beatprompter.events

import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.ui.SongDisplayActivity
import com.stevenfrew.beatprompter.ui.SongListFragment
import com.stevenfrew.beatprompter.ui.pref.SettingsEventHandler

object EventRouter {
	private val songListEventHandlerLock = Any()
	private val songDisplayEventHandlerLock = Any()
	private val settingsEventHandlerLock = Any()
	private val cacheEventHandlerLock = Any()

	private var songListEventHandlers: MutableMap<String, SongListFragment.SongListEventHandler> =
		HashMap()
	private var songDisplayEventHandler: SongDisplayActivity.SongDisplayEventHandler? = null
	private var settingsEventHandler: SettingsEventHandler? = null

	fun addSongListEventHandler(
		key: String,
		songListEventHandler: SongListFragment.SongListEventHandler
	) =
		synchronized(songListEventHandlerLock) {
			songListEventHandlers.put(key, songListEventHandler)
		}

	fun removeSongListEventHandler(key: String) =
		synchronized(songListEventHandlerLock) {
			songListEventHandlers.remove(key)
		}

	fun setSongDisplayEventHandler(songDisplayEventHandler: SongDisplayActivity.SongDisplayEventHandler) =
		synchronized(songDisplayEventHandlerLock) {
			this.songDisplayEventHandler = songDisplayEventHandler
		}

	fun setSettingsEventHandler(settingsEventHandler: SettingsEventHandler?) =
		synchronized(settingsEventHandlerLock) {
			this.settingsEventHandler = settingsEventHandler
		}


	fun sendEventToCache(event: Int, arg: Any) =
		synchronized(cacheEventHandlerLock) {
			Cache.CacheEventHandler.obtainMessage(event, arg).sendToTarget()
		}

	fun sendEventToSettings(event: Int): Boolean =
		synchronized(settingsEventHandlerLock) {
			settingsEventHandler?.obtainMessage(event)?.sendToTarget()
			return settingsEventHandler != null
		}

	fun sendEventToSongList(event: Int): Boolean =
		synchronized(songListEventHandlerLock) {
			songListEventHandlers.values.forEach {
				it.obtainMessage(event).sendToTarget()
			}
			return songListEventHandlers.any()
		}

	fun sendEventToSongList(event: Int, arg: Any): Boolean =
		synchronized(songListEventHandlerLock) {
			songListEventHandlers.values.forEach {
				it.obtainMessage(event, arg).sendToTarget()
			}
			return songListEventHandlers.any()
		}

	fun sendEventToSongList(event: Int, arg1: Int, arg2: Int): Boolean =
		synchronized(songListEventHandlerLock) {
			songListEventHandlers.values.forEach {
				it.obtainMessage(event, arg1, arg2).sendToTarget()
			}
			return songListEventHandlers.any()
		}

	fun sendEventToSongDisplay(event: Int): Boolean =
		synchronized(songDisplayEventHandlerLock) {
			songDisplayEventHandler?.obtainMessage(event)?.sendToTarget()
			return songDisplayEventHandler != null
		}

	fun sendEventToSongDisplay(event: Int, arg: Any): Boolean =
		synchronized(songDisplayEventHandlerLock) {
			songDisplayEventHandler?.obtainMessage(event, arg)?.sendToTarget()
			return songDisplayEventHandler != null
		}

	fun sendEventToSongDisplay(event: Int, arg1: Int, arg2: Int): Boolean =
		synchronized(songDisplayEventHandlerLock) {
			songDisplayEventHandler?.obtainMessage(event, arg1, arg2)?.sendToTarget()
			return songDisplayEventHandler != null
		}
}
