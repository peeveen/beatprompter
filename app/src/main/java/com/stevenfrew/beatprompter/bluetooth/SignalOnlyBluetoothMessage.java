package com.stevenfrew.beatprompter.bluetooth;

abstract class SignalOnlyBluetoothMessage extends BluetoothMessage
{
    private byte mMessageID;
    SignalOnlyBluetoothMessage(byte messageID)
    {
        mMessageID=messageID;
    }

    SignalOnlyBluetoothMessage(byte[] bytes) throws NotEnoughBluetoothDataException
    {
        if(bytes.length<1)
            throw new NotEnoughBluetoothDataException();
        mMessageLength=1;
    }

    public byte[] getBytes()
    {
        return new byte[]{mMessageID};
    }
}
