package com.stevenfrew.beatprompter.comm.bluetooth.message

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.PlayState
import com.stevenfrew.beatprompter.Utils
import com.stevenfrew.beatprompter.comm.OutgoingMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Bluetooth message that instructs the receiver to change the current play mode.
 */
class ToggleStartStopMessage(val mToggleInfo:StartStopToggleInfo) : OutgoingMessage(asBytes(mToggleInfo)) {

    override fun toString(): String {
        return "ToggleStartStopMessage($(mToggleInfo.mStartState),$(mToggleInfo.mTime))"
    }

    companion object {
        internal const val TOGGLE_START_STOP_MESSAGE_ID: Byte = 1

        private fun asBytes(toggleInfo:StartStopToggleInfo):ByteArray {
            return ByteArrayOutputStream().apply {
                write(byteArrayOf(TOGGLE_START_STOP_MESSAGE_ID, toggleInfo.mStartState.asValue().toByte()))
                val longBytes = ByteArray(Utils.LONG_BUFFER_SIZE)
                var time = toggleInfo.mTime
                for (f in 0 until Utils.LONG_BUFFER_SIZE) {
                    longBytes[f] = (time and 0x00000000000000FFL).toByte()
                    time = time shr 8
                }
                write(longBytes)
                flush()
            }.toByteArray()
        }

        @Throws(NotEnoughDataException::class)
        internal fun fromBytes(bytes: ByteArray): ToggleStartStopMessage {
            ByteArrayInputStream(bytes).apply{
                try {
                    var bytesRead = read(ByteArray(1))
                    if (bytesRead == 1) {
                        val startStateBytes = ByteArray(1)
                        read(startStateBytes)
                        val startState = PlayState.fromValue(startStateBytes[0].toInt())
                        var time: Long = 0
                        val longBytes = ByteArray(Utils.LONG_BUFFER_SIZE)
                        bytesRead = read(longBytes)
                        if (bytesRead == Utils.LONG_BUFFER_SIZE) {
                            for (f in Utils.LONG_BUFFER_SIZE - 1 downTo 0) {
                                time = time shl 8
                                time = time or (longBytes[f].toLong() and 0x00000000000000FFL)
                            }
                            return ToggleStartStopMessage(StartStopToggleInfo(startState, time))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(BeatPrompterApplication.TAG, "Couldn't read ToggleStartStopMessage data, assuming insufficient data.", e)
                }
            }
            throw NotEnoughDataException()
        }
    }
}