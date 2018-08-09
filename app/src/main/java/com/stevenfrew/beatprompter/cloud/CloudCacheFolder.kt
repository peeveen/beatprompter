package com.stevenfrew.beatprompter.cloud

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import java.io.File

class CloudCacheFolder internal constructor(parentFolder: File, name: String) : File(parentFolder, name) {

    fun clear() {
        try {
            if (this.exists()) {
                val contents = this.listFiles()
                for (f in contents) {
                    if (!f.isDirectory) {
                        Log.d(BeatPrompterApplication.TAG, "Deleting " + f.absolutePath)
                        if (!f.delete())
                            Log.e(BeatPrompterApplication.TAG, "Failed to delete " + f.absolutePath)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(BeatPrompterApplication.TAG, "Failed to clear cache folder.", e)
        }

    }
}
