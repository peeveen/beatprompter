package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.ImageFile

interface SupportFileResolver {
	fun getMappedAudioFiles(filename: String): List<AudioFile>
	fun getMappedImageFiles(filename: String): List<ImageFile>
}