package com.stevenfrew.beatprompter.bluetooth

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SetSongTimeMessage(time: Long) : BluetoothMessage() {

    var mTime: Long = time

    override val bytes: ByteArray
        get() {
            return ByteArrayOutputStream().apply {
                write(byteArrayOf(SET_SONG_TIME_MESSAGE_ID))
                val longBytes = ByteArray(LONG_BUFFER_SIZE)
                var time = mTime
                for (f in 0 until LONG_BUFFER_SIZE) {
                    longBytes[f] = (time and 0x00000000000000FFL).toByte()
                    time = time shr 8
                }
                write(longBytes)
                flush()
            }.toByteArray()
        }

    companion object {
        internal const val SET_SONG_TIME_MESSAGE_ID: Byte = 2

        @Throws(NotEnoughBluetoothDataException::class)
        internal fun fromBytes(bytes: ByteArray):IncomingBluetoothMessage
        {
            ByteArrayInputStream(bytes).apply {
                try {
                    var bytesRead = read(ByteArray(1))
                    if (bytesRead == 1) {
                        var time:Long = 0
                        val longBytes = ByteArray(LONG_BUFFER_SIZE)
                        bytesRead = read(longBytes)
                        if (bytesRead == LONG_BUFFER_SIZE) {
                            for (f in LONG_BUFFER_SIZE - 1 downTo 0) {
                                time = time shl 8
                                time = time or (longBytes[f].toLong() and 0x00000000000000FFL)
                            }
                            return IncomingBluetoothMessage(SetSongTimeMessage(time),1 + LONG_BUFFER_SIZE)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(BeatPrompterApplication.TAG, "Failed to read SetSongTimeMessage, assuming insufficient data.", e)
                }
            }
            throw NotEnoughBluetoothDataException()
        }
    }
}