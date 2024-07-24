package com.stevenfrew.beatprompter.comm.midi

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.SenderBase

class UsbSender(
	private val mConnection: UsbDeviceConnection,
	private val mEndpoint: UsbEndpoint,
	name: String,
	type: CommunicationType
) : SenderBase(name, type) {
	override fun close() = mConnection.close()

	override fun sendMessageData(bytes: ByteArray, length: Int) {
		mConnection.bulkTransfer(mEndpoint, bytes, length, 5000)
	}
}