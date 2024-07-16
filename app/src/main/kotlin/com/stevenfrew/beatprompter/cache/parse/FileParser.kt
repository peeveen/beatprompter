package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.CachedFile
import org.w3c.dom.Element

/**
 * Base class for all file parsers.
 */
abstract class FileParser<TFileResult>(protected val mCachedCloudFile: CachedFile) {
	protected val mErrors = mutableListOf<FileParseError>()

	abstract fun parse(element: Element? = null): TFileResult

	fun addError(error: FileParseError) = mErrors.add(error)
}