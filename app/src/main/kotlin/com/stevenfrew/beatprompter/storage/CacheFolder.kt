package com.stevenfrew.beatprompter.storage

import com.stevenfrew.beatprompter.BeatPrompterLogger
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
                    BeatPrompterLogger.log("Deleting " + it.absolutePath)
                    if (!it.delete())
                        BeatPrompterLogger.log("Failed to delete " + it.absolutePath)
                }
            }
        } catch (e: Exception) {
            BeatPrompterLogger.log("Failed to clear cache folder.", e)
        }
    }
}
