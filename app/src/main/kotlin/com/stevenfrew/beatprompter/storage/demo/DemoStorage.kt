package com.stevenfrew.beatprompter.storage.demo

import android.app.Activity
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.SongListActivity
import com.stevenfrew.beatprompter.storage.*
import io.reactivex.subjects.PublishSubject
import java.io.*
import java.util.*

/**
 * An implementation of a "storage system" that only contains the demo files.
 */
class DemoStorage(parentActivity: Activity) : Storage(parentActivity, "demo") {
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

    override fun downloadFiles(filesToRefresh: List<FileInfo>, storageListener: StorageListener, itemSource: PublishSubject<DownloadResult>, messageSource: PublishSubject<String>) {
        for (cloudFile in filesToRefresh) {
            try {
                if (cloudFile.mID.equals(DEMO_SONG_TEXT_ID, ignoreCase = true)) {
                    messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.downloading, DEMO_SONG_FILENAME))
                    itemSource.onNext(SuccessfulDownloadResult(cloudFile, createDemoSongTextFile()))
                } else if (cloudFile.mID.equals(DEMO_SONG_AUDIO_ID, ignoreCase = true)) {
                    messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.downloading, DEMO_SONG_AUDIO_FILENAME))
                    itemSource.onNext(SuccessfulDownloadResult(cloudFile, createDemoSongAudioFile()))
                }
            } catch (ioe: IOException) {
                Log.d(BeatPrompterApplication.TAG, "Failed to create demo file", ioe)
                itemSource.onError(ioe)
                return
            }
        }
        itemSource.onComplete()
    }

    override fun readFolderContents(folder: FolderInfo, listener: StorageListener, itemSource: PublishSubject<ItemInfo>, messageSource: PublishSubject<String>, includeSubfolders: Boolean, returnFolders: Boolean) {
        itemSource.onNext(FileInfo(DEMO_SONG_TEXT_ID, DEMO_SONG_FILENAME, Date(), ""))
        itemSource.onNext(FileInfo(DEMO_SONG_AUDIO_ID, DEMO_SONG_AUDIO_FILENAME, Date(), ""))
        itemSource.onComplete()
    }

    private fun createDemoSongTextFile(): File {
        val destinationSongFile = File(cacheFolder, DEMO_SONG_FILENAME)
        val demoFileText = BeatPrompterApplication.getResourceString(R.string.demo_song)
        val bw = BufferedWriter(OutputStreamWriter(FileOutputStream(destinationSongFile)))
        bw.use {
            bw.write(demoFileText)
        }
        return destinationSongFile
    }

    @Throws(IOException::class)
    private fun createDemoSongAudioFile(): File {
        val destinationAudioFile = File(cacheFolder, DEMO_SONG_AUDIO_FILENAME)
        SongListActivity.copyAssetsFileToLocalFolder(DEMO_SONG_AUDIO_FILENAME, destinationAudioFile)
        return destinationAudioFile
    }

    companion object {

        private const val DEMO_SONG_TEXT_ID = "demoSongText"
        private const val DEMO_SONG_AUDIO_ID = "demoSongAudio"
        private const val DEMO_SONG_FILENAME = "demo_song.txt"
        private const val DEMO_SONG_AUDIO_FILENAME = "demo_song.mp3"
    }
}