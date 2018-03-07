package com.stevenfrew.beatprompter;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

abstract class MIDIUSBTask extends Task
{
    private UsbDeviceConnection mUsbDeviceConnection=null;
    private UsbEndpoint mUsbEndpoint=null;
    private final Object connectionSync=new Object();

    void setConnection(UsbDeviceConnection connection,UsbEndpoint endpoint)
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
