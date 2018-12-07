package com.stevenfrew.beatprompter.storage

import com.stevenfrew.beatprompter.Logger
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
                    Logger.log { "Deleting ${it.absolutePath}" }
                    if (!it.delete())
                        Logger.log { "Failed to delete ${it.absolutePath}" }
                }
            }
        } catch (e: Exception) {
            Logger.log("Failed to clear cache folder.", e)
        }
    }
}
