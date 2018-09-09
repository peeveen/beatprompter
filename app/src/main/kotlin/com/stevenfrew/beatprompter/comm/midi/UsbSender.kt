package com.stevenfrew.beatprompter.comm.midi

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.stevenfrew.beatprompter.comm.SenderBase
import com.stevenfrew.beatprompter.comm.midi.message.outgoing.OutgoingMessage

class UsbSender(private val mConnection: UsbDeviceConnection, private val mEndpoint:UsbEndpoint): SenderBase<OutgoingMessage>() {
    override fun close() {
        mConnection.close()
    }

    override fun sendMessageData(bytes: ByteArray, length: Int) {
        mConnection.bulkTransfer(mEndpoint, bytes, length, 60000)
    }
}