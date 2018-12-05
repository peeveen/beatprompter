package com.stevenfrew.beatprompter.storage

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import java.io.File

/**
 * Local cache folder where synchronized files are stored. Typically one of these exists
 * for each storage system.
 */
class CacheFolder internal constructor(parentFolder: File,
                                       name: String)
    : File(parentFolder, name) {

    fun clear() {
        try {
            if (this.exists()) {
                listFiles().filter { !it.isDirectory }.forEach {
                    Log.d(BeatPrompterApplication.TAG, "Deleting " + it.absolutePath)
                    if (!it.delete())
                        Log.e(BeatPrompterApplication.TAG, "Failed to delete " + it.absolutePath)
                }
            }
        } catch (e: Exception) {
            Log.e(BeatPrompterApplication.TAG, "Failed to clear cache folder.", e)
        }
    }
}
