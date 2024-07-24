package com.stevenfrew.beatprompter.storage.demo

import androidx.fragment.app.Fragment
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.storage.DownloadResult
import com.stevenfrew.beatprompter.storage.FailedDownloadResult
import com.stevenfrew.beatprompter.storage.FileInfo
import com.stevenfrew.beatprompter.storage.FolderInfo
import com.stevenfrew.beatprompter.storage.ItemInfo
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageListener
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.storage.SuccessfulDownloadResult
import io.reactivex.subjects.PublishSubject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.Date

/**
 * An implementation of a "storage system" that only contains the demo files.
 */
class DemoStorage(parentFragment: Fragment) : Storage(parentFragment, StorageType.Demo) {
	// No need for region strings here.
	override val cloudStorageName: String
		get() = "demo"

	override val directorySeparator: String
		get() = "/"

	// Nobody will ever see this. Any icon will do.
	override val cloudIconResourceId: Int
		get() = R.drawable.ic_dropbox

	override fun getRootPath(listener: StorageListener, rootPathSource: PublishSubject<FolderInfo>) =
		rootPathSource.onNext(FolderInfo(null, "/", "", ""))

	override fun downloadFiles(
		filesToRefresh: List<FileInfo>,
		storageListener: StorageListener,
		itemSource: PublishSubject<DownloadResult>,
		messageSource: PublishSubject<String>
	) = downloadFiles(filesToRefresh, storageListener, itemSource, messageSource) {
		if (it.id.equals(DEMO_SONG_TEXT_ID, ignoreCase = true))
			SuccessfulDownloadResult(it, createDemoSongTextFile())
		else if (it.id.equals(DEMO_SONG_AUDIO_ID, ignoreCase = true))
			SuccessfulDownloadResult(it, createDemoSongAudioFile())
		else
			FailedDownloadResult(it)
	}

	override fun readFolderContents(
		folder: FolderInfo,
		listener: StorageListener,
		itemSource: PublishSubject<ItemInfo>,
		messageSource: PublishSubject<String>,
		recurseSubFolders: Boolean
	) = with(itemSource) {
		onNext(FileInfo(DEMO_SONG_TEXT_ID, DEMO_SONG_FILENAME, Date()))
		onNext(FileInfo(DEMO_SONG_AUDIO_ID, DEMO_SONG_AUDIO_FILENAME, Date()))
		onComplete()
	}

	private fun createDemoSongTextFile(): File =
		File(cacheFolder, DEMO_SONG_FILENAME).apply {
			val demoFileText = BeatPrompter.appResources.getString(R.string.demo_song)
			BufferedWriter(OutputStreamWriter(FileOutputStream(this))).use {
				it.write(demoFileText)
			}
		}

	private fun createDemoSongAudioFile(): File =
		File(cacheFolder, DEMO_SONG_AUDIO_FILENAME).apply {
			Cache.copyAssetsFileToLocalFolder(DEMO_SONG_AUDIO_FILENAME, this)
		}

	companion object {
		const val DEMO_CACHE_FOLDER_NAME = "demo"
		private const val DEMO_SONG_TEXT_ID = "demoSongText"
		private const val DEMO_SONG_AUDIO_ID = "demoSongAudio"
		private const val DEMO_SONG_FILENAME = "demo_song.txt"
		private const val DEMO_SONG_AUDIO_FILENAME = "demo_song.mp3"
	}
}