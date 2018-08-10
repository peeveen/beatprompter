package com.stevenfrew.beatprompter.bluetooth

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SetSongTimeMessage : BluetoothMessage {

    var mTime: Long = 0

    override val bytes: ByteArray?
        get() {
            try {
                val byteArrayOutputStream = ByteArrayOutputStream()
                byteArrayOutputStream.write(byteArrayOf(SET_SONG_TIME_MESSAGE_ID))
                val longBytes = ByteArray(LONG_BUFFER_SIZE)
                var time = mTime
                for (f in 0 until LONG_BUFFER_SIZE) {
                    longBytes[f] = (time and 0x00000000000000FFL).toByte()
                    time = time shr 8
                }
                byteArrayOutputStream.write(longBytes)
                val byteArray = byteArrayOutputStream.toByteArray()
                byteArrayOutputStream.close()

                return byteArray
            } catch (e: Exception) {
                Log.e(BeatPrompterApplication.TAG, "Failed to write SetSongTimeMessage.", e)
            }

            return null
        }

    constructor(time: Long) {
        mTime = time
    }

    @Throws(NotEnoughBluetoothDataException::class)
    internal constructor(bytes: ByteArray) {
        try {
            val byteArrayInputStream = ByteArrayInputStream(bytes)
            var bytesRead = byteArrayInputStream.read(ByteArray(1))
            if (bytesRead == 1) {
                mTime = 0
                val longBytes = ByteArray(LONG_BUFFER_SIZE)
                bytesRead = byteArrayInputStream.read(longBytes)
                if (bytesRead == LONG_BUFFER_SIZE) {
                    for (f in LONG_BUFFER_SIZE - 1 downTo 0) {
                        mTime = mTime shl 8
                        mTime = mTime or (longBytes[f].toLong() and 0x00000000000000FFL)
                    }
                } else
                    throw NotEnoughBluetoothDataException()
            } else
                throw NotEnoughBluetoothDataException()
            byteArrayInputStream.close()
            mMessageLength = 1 + LONG_BUFFER_SIZE
        } catch (nebde: NotEnoughBluetoothDataException) {
            throw nebde
        } catch (e: Exception) {
            Log.e(BeatPrompterApplication.TAG, "Failed to read SetSongTimeMessage, assuming insufficient data.", e)
            throw NotEnoughBluetoothDataException()
        }
    }

    companion object {
        internal const val SET_SONG_TIME_MESSAGE_ID: Byte = 2
        private const val LONG_BUFFER_SIZE = java.lang.Long.SIZE / java.lang.Byte.SIZE
    }
}