package com.stevenfrew.beatprompter.comm.midi

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.SenderBase

class UsbSender(
	private val connection: UsbDeviceConnection,
	private val endpoint: UsbEndpoint,
	name: String,
	type: CommunicationType
) : SenderBase(name, type) {
	override fun close() = connection.close()

	override fun sendMessageData(bytes: ByteArray, length: Int) {
		connection.bulkTransfer(endpoint, bytes, length, 5000)
	}
}