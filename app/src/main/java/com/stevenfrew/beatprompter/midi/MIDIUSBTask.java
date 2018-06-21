package com.stevenfrew.beatprompter.midi;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import com.stevenfrew.beatprompter.Task;

abstract class MIDIUSBTask extends Task
{
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbEndpoint mUsbEndpoint;
    private final Object connectionSync=new Object();

    public void setConnection(UsbDeviceConnection connection,UsbEndpoint endpoint)
    {
        synchronized (connectionSync)
        {
            mUsbDeviceConnection=connection;
            mUsbEndpoint=endpoint;
        }
    }

    UsbDeviceConnection getConnection()
    {
        synchronized (connectionSync)
        {
            return mUsbDeviceConnection;
        }
    }

    UsbEndpoint getEndpoint()
    {
        synchronized (connectionSync)
        {
            return mUsbEndpoint;
        }
    }

    MIDIUSBTask(UsbDeviceConnection connection, UsbEndpoint endpoint,boolean initialRunningState)
    {
        super(initialRunningState);
        mUsbDeviceConnection=connection;
        mUsbEndpoint=endpoint;
    }
}
