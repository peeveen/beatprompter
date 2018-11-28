package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.comm.OutgoingMessage
import com.stevenfrew.beatprompter.comm.ReceiverBase
import com.stevenfrew.beatprompter.comm.bluetooth.message.*

class Receiver(private val mmSocket: BluetoothSocket) : ReceiverBase(mmSocket.remoteDevice.name) {
    override fun receiveMessageData(buffer: ByteArray, offset: Int, maximumAmount: Int): Int {
        return mmSocket.inputStream.read(buffer, offset, maximumAmount)
    }

    override fun parseMessageData(buffer: ByteArray, dataEnd: Int): Int {
        var bufferCopy = buffer
        var dataStart = 0
        val receivedMessages = mutableListOf<OutgoingMessage>()
        var lastSetTimeMessage: SetSongTimeMessage? = null
        while (dataStart < dataEnd) {
            try {
                val btm = fromBytes(bufferCopy)
                Log.d(BeatPrompterApplication.TAG_COMMS, "Got a fully-formed Bluetooth message.")
                val messageLength = btm.length
                dataStart += messageLength
                bufferCopy = buffer.copyOfRange(dataStart, dataEnd)
                if (btm is SetSongTimeMessage)
                    lastSetTimeMessage = btm
                receivedMessages.add(btm)
            } catch (exception: NotEnoughDataException) {
                // Read again!
                Log.d(BeatPrompterApplication.TAG_COMMS, "Not enough data in the Bluetooth buffer to create a fully formed message, waiting for more data.")
                break
            } catch (exception: UnknownMessageException) {
                Log.d(BeatPrompterApplication.TAG_COMMS, "Unknown Bluetooth message received.")
                ++dataStart // Skip the byte that doesn't match any known message type
            }
        }
        // If we receive multiple SetSongTimeMessages, there is no point in processing any except the last one.
        receivedMessages.filter { it !is SetSongTimeMessage || it == lastSetTimeMessage }.forEach { routeBluetoothMessage(it) }
        return dataStart
    }

    override fun close() {
        mmSocket.close()
    }

    private fun routeBluetoothMessage(msg: OutgoingMessage) {
        when (msg) {
            is ChooseSongMessage -> EventHandler.sendEventToSongList(EventHandler.BLUETOOTH_CHOOSE_SONG, msg.mChoiceInfo)
            is PauseOnScrollStartMessage -> EventHandler.sendEventToSongDisplay(EventHandler.BLUETOOTH_PAUSE_ON_SCROLL_START)
            is QuitSongMessage -> EventHandler.sendEventToSongDisplay(EventHandler.BLUETOOTH_QUIT_SONG)
            is SetSongTimeMessage -> EventHandler.sendEventToSongDisplay(EventHandler.BLUETOOTH_SET_SONG_TIME, msg.mTime)
            is ToggleStartStopMessage -> EventHandler.sendEventToSongDisplay(EventHandler.BLUETOOTH_TOGGLE_START_STOP, msg.mToggleInfo)
        }
    }

    companion object {
        /**
         * Constructs the message object from the received bytes.
         */
        @Throws(NotEnoughDataException::class, UnknownMessageException::class)
        internal fun fromBytes(bytes: ByteArray): OutgoingMessage {
            if (bytes.isNotEmpty())
                return when (bytes[0]) {
                    ChooseSongMessage.CHOOSE_SONG_MESSAGE_ID -> ChooseSongMessage.fromBytes(bytes)
                    ToggleStartStopMessage.TOGGLE_START_STOP_MESSAGE_ID -> ToggleStartStopMessage.fromBytes(bytes)
                    SetSongTimeMessage.SET_SONG_TIME_MESSAGE_ID -> SetSongTimeMessage.fromBytes(bytes)
                    PauseOnScrollStartMessage.PAUSE_ON_SCROLL_START_MESSAGE_ID -> PauseOnScrollStartMessage()
                    QuitSongMessage.QUIT_SONG_MESSAGE_ID -> QuitSongMessage()
                    else -> throw UnknownMessageException()
                }
            throw NotEnoughDataException()
        }
    }
}