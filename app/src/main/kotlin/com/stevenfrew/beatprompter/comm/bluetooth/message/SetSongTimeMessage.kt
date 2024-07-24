package com.stevenfrew.beatprompter.comm.bluetooth.message

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.util.Utils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Bluetooth message that instructs the receiver to change the current song position.
 */
class SetSongTimeMessage(var time: Long) : BluetoothMessage(asBytes(time)) {
	companion object {
		private fun asBytes(t: Long): ByteArray =
			ByteArrayOutputStream().apply {
				write(byteArrayOf(SET_SONG_TIME_MESSAGE_ID))
				val longBytes = ByteArray(Utils.LONG_BUFFER_SIZE)
				var time = t
				repeat(Utils.LONG_BUFFER_SIZE) {
					longBytes[it] = (time and 0x00000000000000FFL).toByte()
					time = time shr 8
				}
				write(longBytes)
				flush()
			}.toByteArray()

		internal fun fromBytes(bytes: ByteArray): SetSongTimeMessage {
			ByteArrayInputStream(bytes).apply {
				try {
					val signalTypeBytesRead = read(ByteArray(1))
					if (signalTypeBytesRead == 1) {
						var time: Long = 0
						val longBytes = ByteArray(Utils.LONG_BUFFER_SIZE)
						val longBytesRead = read(longBytes)
						if (longBytesRead == Utils.LONG_BUFFER_SIZE) {
							for (f in Utils.LONG_BUFFER_SIZE - 1 downTo 0) {
								time = time shl 8
								time = time or (longBytes[f].toLong() and 0x00000000000000FFL)
							}
							return SetSongTimeMessage(time)
						}
					}
				} catch (e: Exception) {
					Logger.logComms({ "Failed to read SetSongTimeMessage, assuming insufficient data." }, e)
				}
			}
			throw NotEnoughDataException()
		}
	}
}