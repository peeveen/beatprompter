package com.stevenfrew.beatprompter.comm.bluetooth.message

/**
 * A received Bluetooth message.
 */
internal data class IncomingMessage constructor(val receivedMessage: Message, val messageLength:Int)
{
    companion object {
        /**
         * Constructs the message object from the received bytes.
         */
        @Throws(NotEnoughDataException::class, UnknownMessageException::class)
        internal fun fromBytes(bytes: ByteArray): IncomingMessage {
            if (bytes.isNotEmpty())
                return when(bytes[0]) {
                    ChooseSongMessage.CHOOSE_SONG_MESSAGE_ID -> ChooseSongMessage.fromBytes(bytes)
                    ToggleStartStopMessage.TOGGLE_START_STOP_MESSAGE_ID -> ToggleStartStopMessage.fromBytes(bytes)
                    SetSongTimeMessage.SET_SONG_TIME_MESSAGE_ID -> SetSongTimeMessage.fromBytes(bytes)
                    PauseOnScrollStartMessage.PAUSE_ON_SCROLL_START_MESSAGE_ID -> IncomingMessage(PauseOnScrollStartMessage(), 1)
                    QuitSongMessage.QUIT_SONG_MESSAGE_ID -> IncomingMessage(QuitSongMessage(), 1)
                    else -> throw UnknownMessageException()
                }
            throw NotEnoughDataException()
        }
    }
}