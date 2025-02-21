package com.stevenfrew.beatprompter.util

import android.graphics.Rect
import android.graphics.RectF
import android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import com.stevenfrew.beatprompter.ui.BeatCounterTextOverlay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar

fun String.splitAndTrim(separator: String): List<String> =
	split(separator).run {
		mapNotNull {
			it.trim().ifEmpty { null }
		}
	}

/**
 * Remove stupid BOF character
 */
fun String.removeControlCharacters(): String = replace("\uFEFF", "")

fun RectF.inflate(amount: Int): RectF =
	RectF(this.left - amount, this.top - amount, this.right + amount, this.bottom + amount)

fun Rect.inflate(amount: Int): Rect =
	Rect(this.left - amount, this.top - amount, this.right + amount, this.bottom + amount)

/**
 * Replaces weird apostrophe with usual apostrophe ... prevents failed matches based on apostrophe difference.
 */
fun String.normalize(): String = replace('’', '\'').removeControlCharacters().lowercase()

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
	} catch (_: Exception) {
		// Wasn't decimal
		false
	}
}

fun String.stripHexSignifiers(): String =
	lowercase().run {
		if (startsWith("0x"))
			substringAfter("0x")
		else if (endsWith("h"))
			substring(0, length - 1)
		else
			this
	}

fun String.characters(): List<String> = toCharArray().map { it.toString() }

fun List<Any?>.flattenAll(): List<Any?> =
	mutableListOf<Any?>().also {
		forEach { e ->
			when (e) {
				!is List<Any?> -> it.add(e)
				else -> it.addAll(e.flattenAll())
			}
		}
	}

fun <TParameters, TProgress, TResult> CoroutineTask<TParameters, TProgress, TResult>.execute(
	params: TParameters,
	preExecuteContext: CoroutineDispatcher? = null
) =
	launch {
		if (preExecuteContext == null)
			onPreExecute()
		else
			withContext(preExecuteContext) { onPreExecute() }
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

fun File.getMd5Hash(): String = getHash("MD5").toHashString(32)
fun File.getHash(algorithm: String) =
	(if (exists()) readBytes() else ByteArray(0)).getHash(algorithm)

fun ByteArray.toHashString(minLength: Int) =
	BigInteger(1, this).toString(16).padStart(minLength, '0')

fun ByteArray.getHash(algorithm: String) = MessageDigest.getInstance(algorithm).digest(this)

val timeFormatter = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)
fun BeatCounterTextOverlay.getTextOverlayFn(songTitle: String): () -> String =
	when (this) {
		BeatCounterTextOverlay.Nothing -> {
			{
				""
			}
		}

		BeatCounterTextOverlay.SongTitle -> {
			{
				songTitle
			}
		}

		BeatCounterTextOverlay.CurrentTime -> {
			{
				timeFormatter.format(Calendar.getInstance().time)
			}
		}
	}
