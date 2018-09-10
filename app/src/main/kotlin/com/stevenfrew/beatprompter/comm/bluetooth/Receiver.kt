package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.comm.OutgoingMessage
import com.stevenfrew.beatprompter.comm.ReceiverBase
import com.stevenfrew.beatprompter.comm.bluetooth.message.*

class Receiver(private val mmSocket: BluetoothSocket): ReceiverBase() {
    override fun receiveMessageData(buffer: ByteArray, offset: Int, maximumAmount: Int): Int {
        return mmSocket.inputStream.read(buffer, offset, maximumAmount)
    }

    override fun parseMessageData(buffer: ByteArray, dataStart: Int, dataEnd: Int): Int {
        var bufferCopy=buffer
        var bufferStart=dataStart
        while (dataStart < dataEnd) {
            try {
                val btm = fromBytes(bufferCopy)
                val messageLength = btm.length
                bufferStart += messageLength
                bufferCopy=buffer.copyOfRange(bufferStart,dataEnd)
                routeBluetoothMessage(btm)
            } catch (exception: NotEnoughDataException) {
                // Read again!
                Log.d(BluetoothManager.BLUETOOTH_TAG, "Not enough data in the Bluetooth buffer to create a fully formed message, waiting for more data.")
                return 0
            } catch (exception: UnknownMessageException) {
                Log.d(BluetoothManager.BLUETOOTH_TAG, "Unknown Bluetooth message received.")
                return 1
            }
        }
        return bufferStart-dataStart
    }

    override fun close() {
        mmSocket.close()
    }

    private fun routeBluetoothMessage(msg:OutgoingMessage)
    {
        when (msg) {
            is ChooseSongMessage -> EventHandler.sendEventToSongList(EventHandler.BLUETOOTH_CHOOSE_SONG, msg.mChoiceInfo)
            is PauseOnScrollStartMessage -> EventHandler.sendEventToSongDisplay(EventHandler.BLUETOOTH_PAUSE_ON_SCROLL_START)
            is QuitSongMessage -> EventHandler.sendEventToSongDisplay(EventHandler.BLUETOOTH_QUIT_SONG)
            is SetSongTimeMessage -> EventHandler.sendEventToSongDisplay(EventHandler.BLUETOOTH_SET_SONG_TIME,msg.mTime)
            is ToggleStartStopMessage -> EventHandler.sendEventToSongDisplay(EventHandler.BLUETOOTH_TOGGLE_START_STOP,msg.mToggleInfo)
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