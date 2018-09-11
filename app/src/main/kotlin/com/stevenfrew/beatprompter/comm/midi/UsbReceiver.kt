package com.stevenfrew.beatprompter.comm.midi

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint

class UsbReceiver(private val mConnection: UsbDeviceConnection, private val mEndpoint: UsbEndpoint,name:String): Receiver(name) {
    init
    {
        // Read all incoming data until there is none left. Basically, clear anything that
        // has been buffered before we accepted the connection.
        // On the off-chance that there is a never-ending barrage of data coming in at a ridiculous
        // speed, let's say 1K of data, max, to prevent lockup.
        var bufferClear = 0
        var dataRead: Int
        val wasteBuffer=ByteArray(mEndpoint.maxPacketSize)
        do {
            dataRead = mConnection.bulkTransfer(mEndpoint, wasteBuffer, wasteBuffer.size, 250)
            if (dataRead > 0)
                bufferClear += dataRead
        } while (dataRead > 0 && bufferClear < 1024)
    }

    override fun close() {
        mConnection.close()
    }

    override fun receiveMessageData(buffer: ByteArray, offset: Int, maximumAmount: Int): Int {
        val newArray = if (offset == 0) buffer else ByteArray(maximumAmount)
        val dataRead = mConnection.bulkTransfer(mEndpoint, newArray, maximumAmount, 1000)
        if (offset != 0)
            System.arraycopy(newArray, 0, buffer, offset, maximumAmount)
        return dataRead
    }
}