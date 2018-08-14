package com.stevenfrew.beatprompter.bluetooth

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.PlayState
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ToggleStartStopMessage(startState: PlayState, time: Long) : BluetoothMessage() {

    var mStartState: PlayState = startState
    var mTime: Long = time

    override val bytes: ByteArray?
        get() {
            try {
                val byteArrayOutputStream = ByteArrayOutputStream()
                byteArrayOutputStream.write(byteArrayOf(TOGGLE_START_STOP_MESSAGE_ID, mStartState.asValue().toByte()))
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
                Log.e(BeatPrompterApplication.TAG, "Couldn't write ToggleStartStopMessage data", e)
            }

            return null
        }

    override fun toString(): String {
        return "ToggleStartStopMessage($mStartState,$mTime)"
    }

    companion object {
        internal const val TOGGLE_START_STOP_MESSAGE_ID: Byte = 1

        private const val LONG_BUFFER_SIZE = java.lang.Long.SIZE / java.lang.Byte.SIZE

        @Throws(NotEnoughBluetoothDataException::class)
        internal fun fromBytes(bytes: ByteArray):IncomingBluetoothMessage {
            val byteArrayInputStream = ByteArrayInputStream(bytes)
            try {
                var bytesRead = byteArrayInputStream.read(ByteArray(1))
                if (bytesRead == 1) {
                    val startStateBytes = ByteArray(1)

                    byteArrayInputStream.read(startStateBytes)
                    val startState = PlayState.fromValue(startStateBytes[0].toInt())
                    var time: Long = 0
                    val longBytes = ByteArray(LONG_BUFFER_SIZE)
                    bytesRead = byteArrayInputStream.read(longBytes)
                    if (bytesRead == LONG_BUFFER_SIZE) {
                        for (f in LONG_BUFFER_SIZE - 1 downTo 0) {
                            time = time shl 8
                            time = time or (longBytes[f].toLong() and 0x00000000000000FFL)
                        }
                        return IncomingBluetoothMessage(ToggleStartStopMessage(startState, time), 2 + LONG_BUFFER_SIZE)
                    }
                }
                throw NotEnoughBluetoothDataException()
            } catch (e: Exception) {
                Log.e(BeatPrompterApplication.TAG, "Couldn't read ToggleStartStopMessage data, assuming insufficient data.", e)
                throw NotEnoughBluetoothDataException()
            } finally {
                byteArrayInputStream.close()
            }
        }
    }
}
