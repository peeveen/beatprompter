package com.stevenfrew.beatprompter.comm.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.SenderBase

@SuppressLint("MissingPermission") // The method that uses this constructor checks for SecurityException.
class Sender(private val mClientSocket: BluetoothSocket, type: CommunicationType) :
	SenderBase(mClientSocket.remoteDevice.name, type) {
	override fun sendMessageData(bytes: ByteArray, length: Int) =
		mClientSocket.outputStream.write(
			if (bytes.size == length) bytes else bytes.copyOfRange(
				0,
				length
			)
		)

	override fun close() = mClientSocket.close()
}