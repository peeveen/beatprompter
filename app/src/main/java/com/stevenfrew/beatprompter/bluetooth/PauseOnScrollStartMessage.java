package com.stevenfrew.beatprompter.bluetooth;

public class PauseOnScrollStartMessage extends SignalOnlyBluetoothMessage
{
    static final byte PAUSE_ON_SCROLL_START_MESSAGE_ID=3;

    public PauseOnScrollStartMessage()
    {
        super(PAUSE_ON_SCROLL_START_MESSAGE_ID);
    }

    PauseOnScrollStartMessage(byte[] bytes) throws NotEnoughBluetoothDataException
    {
        super(bytes);
    }
}
