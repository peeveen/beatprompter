package com.stevenfrew.beatprompter.bluetooth

/**
 * Base class for sent/received Bluetooth messages.
 */
abstract class BluetoothMessage {
    abstract val bytes: ByteArray

    companion object {
        /**
         * Constructs the message object from the received bytes.
         */
        @Throws(NotEnoughBluetoothDataException::class,UnknownBluetoothMessageException::class)
        internal fun fromBytes(bytes: ByteArray): IncomingBluetoothMessage {
            if (bytes.isNotEmpty())
                when(bytes[0]) {
                    ChooseSongMessage.CHOOSE_SONG_MESSAGE_ID -> return ChooseSongMessage.fromBytes(bytes)
                    ToggleStartStopMessage.TOGGLE_START_STOP_MESSAGE_ID -> return ToggleStartStopMessage.fromBytes(bytes)
                    SetSongTimeMessage.SET_SONG_TIME_MESSAGE_ID -> return SetSongTimeMessage.fromBytes(bytes)
                    PauseOnScrollStartMessage.PAUSE_ON_SCROLL_START_MESSAGE_ID -> return IncomingBluetoothMessage(PauseOnScrollStartMessage(),1)
                    QuitSongMessage.QUIT_SONG_MESSAGE_ID -> return IncomingBluetoothMessage(QuitSongMessage(),1)
                }
            throw UnknownBluetoothMessageException()
        }

        // Size of a "long", in bytes.
        internal const val LONG_BUFFER_SIZE = 8
    }
}