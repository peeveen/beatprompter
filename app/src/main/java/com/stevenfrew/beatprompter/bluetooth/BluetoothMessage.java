package com.stevenfrew.beatprompter.bluetooth;

public abstract class BluetoothMessage
{
    public abstract byte[] getBytes();
    public int mMessageLength;

    static BluetoothMessage fromBytes(byte[] bytes) throws NotEnoughBluetoothDataException
    {
        if((bytes!=null)&&(bytes.length>0))
        {
            if (bytes[0] == ChooseSongMessage.CHOOSE_SONG_MESSAGE_ID)
                return new ChooseSongMessage(bytes);
            else if (bytes[0] == ToggleStartStopMessage.TOGGLE_START_STOP_MESSAGE_ID)
                return new ToggleStartStopMessage(bytes);
            else if (bytes[0] == SetSongTimeMessage.SET_SONG_TIME_MESSAGE_ID)
                return new SetSongTimeMessage(bytes);
            else if (bytes[0] == PauseOnScrollStartMessage.PAUSE_ON_SCROLL_START_MESSAGE_ID)
                return new PauseOnScrollStartMessage(bytes);
            else if (bytes[0] == QuitSongMessage.QUIT_SONG_MESSAGE_ID)
                return new QuitSongMessage(bytes);
        }
        return null;
    }
}
