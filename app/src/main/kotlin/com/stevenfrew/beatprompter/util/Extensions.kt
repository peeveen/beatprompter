package com.stevenfrew.beatprompter.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun String.splitAndTrim(separator: String): List<String> {
	val bits = split(separator)
	return bits.mapNotNull()
	{
		val trimmed = it.trim()
		if (trimmed.isEmpty())
			null
		else
			trimmed
	}
}

/**
 * Remove stupid BOF character
 */
fun String.removeControlCharacters(): String {
	return replace("\uFEFF", "")
}

/**
 * Replaces weird apostrophe with usual apostrophe ... prevents failed matches based on apostrophe difference.
 */
fun String.normalize(): String {
	return replace('’', '\'').removeControlCharacters().lowercase()
}

fun String?.looksLikeHex(): Boolean {
	if (this == null)
		return false
	val strippedString = stripHexSignifiers()
	// If there was no signifier, then if it only contains
	// numbers, it's probably decimal.
	if (strippedString.length == length)
		if (looksLikeDecimal())
			return false
	// Hex values for this app are two-chars long, max.
	return strippedString.matches(Regex("[0-9a-f]{1,2}"))
}

fun String?.looksLikeDecimal(): Boolean {
	if (this == null)
		return false
	return try {
		toInt()
		true
	} catch (e: Exception) {
		// Wasn't decimal
		false
	}
}

fun String.stripHexSignifiers(): String {
	val str = lowercase()
	if (str.startsWith("0x"))
		return str.substringAfter("0x")
	else if (str.endsWith("h"))
		return str.substring(0, str.length - 1)
	return str
}

fun String.characters(): List<String> {
	return toCharArray().map { it.toString() }
}

fun List<Any?>.flattenAll(): List<Any?> {
	val output = mutableListOf<Any?>()
	forEach { e ->
		when (e) {
			!is List<Any?> -> output.add(e)
			else -> output.addAll(e.flattenAll())
		}
	}
	return output
}

fun <TParameters, TProgress, TResult> CoroutineTask<TParameters, TProgress, TResult>.execute(params: TParameters) =
	launch {
		onPreExecute()
		withContext(Dispatchers.IO) {
			try {
				val result = doInBackground(params) {
					withContext(Dispatchers.Main) { onProgressUpdate(it) }
				}
				withContext(Dispatchers.Main) { onPostExecute(result) }
			} catch (t: Throwable) {
				withContext(Dispatchers.Main) { onError(t) }
			}
		}
	}