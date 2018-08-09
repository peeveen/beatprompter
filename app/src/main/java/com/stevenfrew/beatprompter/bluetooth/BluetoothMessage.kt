package com.stevenfrew.beatprompter.bluetooth

abstract class BluetoothMessage {
    abstract val bytes: ByteArray?
    internal var mMessageLength: Int = 0

    companion object {
        @Throws(NotEnoughBluetoothDataException::class)
        internal fun fromBytes(bytes: ByteArray?): BluetoothMessage? {
            if (bytes != null && bytes.isNotEmpty())
                when(bytes[0]) {
                    ChooseSongMessage.CHOOSE_SONG_MESSAGE_ID -> return ChooseSongMessage(bytes)
                    ToggleStartStopMessage.TOGGLE_START_STOP_MESSAGE_ID -> return ToggleStartStopMessage(bytes)
                    SetSongTimeMessage.SET_SONG_TIME_MESSAGE_ID -> return SetSongTimeMessage(bytes)
                    PauseOnScrollStartMessage.PAUSE_ON_SCROLL_START_MESSAGE_ID -> return PauseOnScrollStartMessage()
                    QuitSongMessage.QUIT_SONG_MESSAGE_ID -> return QuitSongMessage()
                }
            return null
        }
    }
}