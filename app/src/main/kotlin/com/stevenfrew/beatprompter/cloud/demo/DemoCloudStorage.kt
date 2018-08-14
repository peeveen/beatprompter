package com.stevenfrew.beatprompter.cloud.demo

import android.app.Activity
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.SongList
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
            if (cloudFile.mID.equals(DEMO_SONG_TEXT_ID, ignoreCase = true)) {
                messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.downloading, DEMO_SONG_FILENAME))
                itemSource.onNext(SuccessfulCloudDownloadResult(cloudFile, createDemoSongTextFile()!!))
            } else if (cloudFile.mID.equals(DEMO_SONG_AUDIO_ID, ignoreCase = true))
                try {
                    messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.downloading, DEMO_SONG_AUDIO_FILENAME))
                    itemSource.onNext(SuccessfulCloudDownloadResult(cloudFile, createDemoSongAudioFile()))
                } catch (ioe: IOException) {
                    itemSource.onError(ioe)
                }

        }
        itemSource.onComplete()
    }

    override fun readFolderContents(folder: CloudFolderInfo, listener: CloudListener, itemSource: PublishSubject<CloudItemInfo>, messageSource: PublishSubject<String>, includeSubfolders: Boolean, returnFolders: Boolean) {
        itemSource.onNext(CloudFileInfo(DEMO_SONG_TEXT_ID, DEMO_SONG_FILENAME, Date(), ""))
        itemSource.onNext(CloudFileInfo(DEMO_SONG_AUDIO_ID, DEMO_SONG_AUDIO_FILENAME, Date(), ""))
        itemSource.onComplete()
    }

    private fun createDemoSongTextFile(): File? {
        val demoFileText = BeatPrompterApplication.getResourceString(R.string.demo_song)
        var destinationSongFile: File? = File(cacheFolder, DEMO_SONG_FILENAME)
        var bw: BufferedWriter? = null
        try {
            bw = BufferedWriter(OutputStreamWriter(FileOutputStream(destinationSongFile!!)))
            bw.write(demoFileText)
        } catch (e: Exception) {
            Log.d(BeatPrompterApplication.TAG, "Failed to create demo file", e)
            destinationSongFile = null
        } finally {
            if (bw != null)
                try {
                    bw.close()
                } catch (e: Exception) {
                    Log.d(BeatPrompterApplication.TAG, "Failed to close demo file", e)
                }

        }
        return destinationSongFile
    }

    @Throws(IOException::class)
    private fun createDemoSongAudioFile(): File {
        val destinationAudioFile = File(cacheFolder, DEMO_SONG_AUDIO_FILENAME)
        SongList.copyAssetsFileToLocalFolder(DEMO_SONG_AUDIO_FILENAME, destinationAudioFile)
        return destinationAudioFile
    }

    companion object {

        private const val DEMO_SONG_TEXT_ID = "demoSongText"
        private const val DEMO_SONG_AUDIO_ID = "demoSongAudio"
        private const val DEMO_SONG_FILENAME = "demo_song.txt"
        private const val DEMO_SONG_AUDIO_FILENAME = "demo_song.mp3"
    }
}