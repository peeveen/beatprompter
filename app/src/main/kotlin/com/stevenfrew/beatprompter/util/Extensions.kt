package com.stevenfrew.beatprompter.util

import android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
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

fun UsbDevice.getUsbDeviceMidiInterface(): UsbInterface? {
	val interfaceCount = interfaceCount
	var fallbackInterface: UsbInterface? = null
	repeat(interfaceCount) { interfaceIndex ->
		val face = getInterface(interfaceIndex)
		val mainClass = face.interfaceClass
		val subclass = face.interfaceSubclass
		// Oh you f***in beauty, we've got a perfect compliant MIDI interface!
		if (mainClass == 1 && subclass == 3)
			return face
		else if (mainClass == 255 && fallbackInterface == null) {
			// Basically, go with this if:
			// It has all endpoints of type "bulk transfer"
			// and
			// The endpoints have a max packet size that is a multiplier of 4.
			val endPointCount = face.endpointCount
			var allEndpointsCheckout = true
			repeat(endPointCount) {
				val ep = face.getEndpoint(it)
				val maxPacket = ep.maxPacketSize
				val type = ep.type
				allEndpointsCheckout =
					allEndpointsCheckout and (type == USB_ENDPOINT_XFER_BULK && (maxPacket and 3) == 0)
			}
			if (allEndpointsCheckout)
				fallbackInterface = face
		}
		// Aw bollocks, we've got some vendor-specific pish.
		// Still worth trying.
	}
	return fallbackInterface
}