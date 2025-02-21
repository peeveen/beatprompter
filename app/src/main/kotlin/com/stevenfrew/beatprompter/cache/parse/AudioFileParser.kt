package com.stevenfrew.beatprompter.cache.parse

import android.media.MediaMetadataRetriever
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.util.Utils
import org.w3c.dom.Element

/**
 * "Parses" audio files. Basically validates that the file IS ACTUALLY an audio file.
 */
class AudioFileParser(private val cachedCloudFile: CachedFile) : ContentParser<AudioFile>() {
	override fun parse(element: Element?): AudioFile =
		try {
			// Try to read the length of the track. If it fails, it's not an audio file.
			AudioFile.readAudioFileLengthFromAttribute(element)?.let {
				AudioFile(cachedCloudFile, it)
			} ?: MediaMetadataRetriever().run {
				setDataSource(cachedCloudFile.file.absolutePath)
				extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
					AudioFile(cachedCloudFile, Utils.milliToNano(it.toLong()))
				}
			}
		} catch (_: Exception) {
			// Not bothered about what the exception is ... file is obviously shite.
			null
		} ?: throw InvalidBeatPrompterFileException(R.string.notAnAudioFile, cachedCloudFile.name)
}
