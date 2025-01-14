package com.stevenfrew.beatprompter.mock

import android.util.Size
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.ImageFile
import com.stevenfrew.beatprompter.cache.parse.SupportFileResolver
import java.io.File
import java.nio.file.Paths
import java.util.Date
import kotlin.io.path.pathString

class MockSupportFileResolver(private val testDataFolderPath: String) : SupportFileResolver {
	override fun getMappedAudioFiles(filename: String): List<AudioFile> =
		listOf(
			AudioFile(
				CachedFile(
					File(Paths.get(testDataFolderPath, "audio", "mock_audio.mp3").pathString),
					filename,
					filename,
					Date(),
					"testContentHash",
					listOf()
				), 200000
			)
		)

	override fun getMappedImageFiles(filename: String): List<ImageFile> =
		listOf(
			ImageFile(
				CachedFile(
					File(Paths.get(testDataFolderPath, "images", "mock_image.jpg").pathString),
					filename,
					filename,
					Date(),
					"testContentHash",
					listOf()
				),
				Size(100, 100)
			)
		)
}