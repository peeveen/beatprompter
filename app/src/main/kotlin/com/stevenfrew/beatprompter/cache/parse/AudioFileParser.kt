package com.stevenfrew.beatprompter.cache.parse

import android.media.MediaMetadataRetriever
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor

/**
 * "Parses" audio files. Basically validates that the file IS ACTUALLY an audio file.
 */
class AudioFileParser constructor(cachedCloudFileDescriptor:CachedCloudFileDescriptor):FileParser<AudioFile>(cachedCloudFileDescriptor) {

    override fun parse(): AudioFile {
        try {
            // Try to read the length of the track. If it fails, it's not an audio file.
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(mCachedCloudFileDescriptor.mFile.absolutePath)
            val data = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (data != null)
                return AudioFile(mCachedCloudFileDescriptor, Utils.milliToNano(data.toLong()))
        } catch (e: Exception) {
            // Not bothered about what the exception is ... file is obviously shite.
        }
        throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.notAnAudioFile, mCachedCloudFileDescriptor.mName))
    }
}