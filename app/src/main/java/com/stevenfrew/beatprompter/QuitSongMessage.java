package com.stevenfrew.beatprompter;

class QuitSongMessage extends SignalOnlyBluetoothMessage
{
    static final byte QUIT_SONG_MESSAGE_ID=4;

    QuitSongMessage()
    {
        super(QUIT_SONG_MESSAGE_ID);
    }

    QuitSongMessage(byte[] bytes) throws NotEnoughBluetoothDataException
    {
        super(bytes);
    }
}
