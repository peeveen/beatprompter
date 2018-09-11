package com.stevenfrew.beatprompter.cloud.demo

import android.app.Activity
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.SongListActivity
import com.stevenfrew.beatprompter.cloud.*
import io.reactivex.subjects.PublishSubject
import java.io.*
import java.util.*

class DemoCloudStorage(parentActivity: Activity) : CloudStorage(parentActivity, "demo") {
    // No need for region strings here.
    override val cloudStorageName: String
        get() = "demo"

    override val directorySeparator: String
        get() = "/"

    // Nobody will ever see this. Any icon will do.
    override val cloudIconResourceId: Int
        get() = R.drawable.ic_dropbox

    override fun getRootPath(listener: CloudListener, rootPathSource: PublishSubject<CloudFolderInfo>) {
        rootPathSource.onNext(CloudFolderInfo(null, "/", "", ""))
    }

    override fun downloadFiles(filesToRefresh: List<CloudFileInfo>, cloudListener: CloudListener, itemSource: PublishSubject<CloudDownloadResult>, messageSource: PublishSubject<String>) {
        for (cloudFile in filesToRefresh) {
            try {
                if (cloudFile.mID.equals(DEMO_SONG_TEXT_ID, ignoreCase = true)) {
                    messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.downloading, DEMO_SONG_FILENAME))
                    itemSource.onNext(SuccessfulCloudDownloadResult(cloudFile, createDemoSongTextFile()))
                } else if (cloudFile.mID.equals(DEMO_SONG_AUDIO_ID, ignoreCase = true)) {
                    messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.downloading, DEMO_SONG_AUDIO_FILENAME))
                    itemSource.onNext(SuccessfulCloudDownloadResult(cloudFile, createDemoSongAudioFile()))
                }
            }
            catch(ioe:IOException)
            {
                Log.d(BeatPrompterApplication.TAG, "Failed to create demo file", ioe)
                itemSource.onError(ioe)
                return
            }
        }
        itemSource.onComplete()
    }

    override fun readFolderContents(folder: CloudFolderInfo, listener: CloudListener, itemSource: PublishSubject<CloudItemInfo>, messageSource: PublishSubject<String>, includeSubfolders: Boolean, returnFolders: Boolean) {
        itemSource.onNext(CloudFileInfo(DEMO_SONG_TEXT_ID, DEMO_SONG_FILENAME, Date(), ""))
        itemSource.onNext(CloudFileInfo(DEMO_SONG_AUDIO_ID, DEMO_SONG_AUDIO_FILENAME, Date(), ""))
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