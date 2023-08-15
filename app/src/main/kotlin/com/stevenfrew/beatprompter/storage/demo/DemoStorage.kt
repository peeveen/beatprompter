package com.stevenfrew.beatprompter.storage.demo

import androidx.fragment.app.Fragment
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.storage.DownloadResult
import com.stevenfrew.beatprompter.storage.FileInfo
import com.stevenfrew.beatprompter.storage.FolderInfo
import com.stevenfrew.beatprompter.storage.ItemInfo
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageListener
import com.stevenfrew.beatprompter.storage.SuccessfulDownloadResult
import com.stevenfrew.beatprompter.ui.SongListFragment
import io.reactivex.subjects.PublishSubject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.Date

/**
 * An implementation of a "storage system" that only contains the demo files.
 */
class DemoStorage(parentFragment: Fragment) : Storage(parentFragment, "demo") {
	// No need for region strings here.
	override val cloudStorageName: String
		get() = "demo"

	override val directorySeparator: String
		get() = "/"

	// Nobody will ever see this. Any icon will do.
	override val cloudIconResourceId: Int
		get() = R.drawable.ic_dropbox

	override fun getRootPath(listener: StorageListener, rootPathSource: PublishSubject<FolderInfo>) {
		rootPathSource.onNext(FolderInfo(null, "/", "", ""))
	}

	override fun downloadFiles(
		filesToRefresh: List<FileInfo>,
		storageListener: StorageListener,
		itemSource: PublishSubject<DownloadResult>,
		messageSource: PublishSubject<String>
	) {
		for (cloudFile in filesToRefresh) {
			try {
				if (cloudFile.mID.equals(DEMO_SONG_TEXT_ID, ignoreCase = true)) {
					messageSource.onNext(
						BeatPrompter.getResourceString(
							R.string.downloading,
							DEMO_SONG_FILENAME
						)
					)
					itemSource.onNext(SuccessfulDownloadResult(cloudFile, createDemoSongTextFile()))
				} else if (cloudFile.mID.equals(DEMO_SONG_AUDIO_ID, ignoreCase = true)) {
					messageSource.onNext(
						BeatPrompter.getResourceString(
							R.string.downloading,
							DEMO_SONG_AUDIO_FILENAME
						)
					)
					itemSource.onNext(SuccessfulDownloadResult(cloudFile, createDemoSongAudioFile()))
				}
			} catch (ioe: IOException) {
				Logger.log("Failed to create demo file", ioe)
				itemSource.onError(ioe)
				return
			}
		}
		itemSource.onComplete()
	}

	override fun readFolderContents(
		folder: FolderInfo,
		listener: StorageListener,
		itemSource: PublishSubject<ItemInfo>,
		messageSource: PublishSubject<String>,
		recurseSubfolders: Boolean
	) {
		itemSource.apply {
			onNext(FileInfo(DEMO_SONG_TEXT_ID, DEMO_SONG_FILENAME, Date()))
			onNext(FileInfo(DEMO_SONG_AUDIO_ID, DEMO_SONG_AUDIO_FILENAME, Date()))
			onComplete()
		}
	}

	private fun createDemoSongTextFile(): File {
		val destinationSongFile = File(cacheFolder, DEMO_SONG_FILENAME)
		val demoFileText = BeatPrompter.getResourceString(R.string.demo_song)
		val bw = BufferedWriter(OutputStreamWriter(FileOutputStream(destinationSongFile)))
		bw.use {
			bw.write(demoFileText)
		}
		return destinationSongFile
	}

	private fun createDemoSongAudioFile(): File {
		val destinationAudioFile = File(cacheFolder, DEMO_SONG_AUDIO_FILENAME)
		SongListFragment.copyAssetsFileToLocalFolder(DEMO_SONG_AUDIO_FILENAME, destinationAudioFile)
		return destinationAudioFile
	}

	companion object {
		private const val DEMO_SONG_TEXT_ID = "demoSongText"
		private const val DEMO_SONG_AUDIO_ID = "demoSongAudio"
		private const val DEMO_SONG_FILENAME = "demo_song.txt"
		private const val DEMO_SONG_AUDIO_FILENAME = "demo_song.mp3"
	}
}