package com.stevenfrew.beatprompter.cache.parse

import android.media.MediaMetadataRetriever
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor

class AudioFileParser constructor(cachedCloudFileDescriptor:CachedCloudFileDescriptor):FileParser<AudioFile>(cachedCloudFileDescriptor) {

    override fun parse(): AudioFile {
        try {
            // Try to read the length of the track. If it fails, it's not an audio file.
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(mCachedCloudFileDescriptor.mFile.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        } catch (e: Exception) {
            throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.notAnAudioFile, mCachedCloudFileDescriptor.mName))
        }
        return AudioFile(mCachedCloudFileDescriptor)
    }
}