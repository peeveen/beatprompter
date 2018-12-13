package com.stevenfrew.beatprompter.cache.parse

import android.media.MediaMetadataRetriever
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.CachedFileDescriptor

/**
 * "Parses" audio files. Basically validates that the file IS ACTUALLY an audio file.
 */
class AudioFileParser(cachedCloudFileDescriptor: CachedFileDescriptor)
    : FileParser<AudioFile>(cachedCloudFileDescriptor) {

    override fun parse(): AudioFile {
        try {
            // Try to read the length of the track. If it fails, it's not an audio file.
            MediaMetadataRetriever().apply {
                setDataSource(mCachedCloudFileDescriptor.mFile.absolutePath)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.also {
                    return AudioFile(mCachedCloudFileDescriptor, Utils.milliToNano(it.toLong()))
                }
            }
        } catch (e: Exception) {
            // Not bothered about what the exception is ... file is obviously shite.
        }
        throw InvalidBeatPrompterFileException(R.string.notAnAudioFile, mCachedCloudFileDescriptor.mName)
    }
}