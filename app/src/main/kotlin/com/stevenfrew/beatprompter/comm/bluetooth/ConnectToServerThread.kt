package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.util.Utils
import java.io.IOException
import java.util.UUID

/**
 * A thread that continuously attempts to connect to a band leader.
 */
internal class ConnectToServerThread(
	private val mDevice: BluetoothDevice,
	private val mUUID: UUID,
	private val mOnConnectedFunction: (socket: BluetoothSocket) -> Unit
) : Thread() {
	private var mmSocket: BluetoothSocket? = null
	private var mStop = false

	override fun run() {
		while (!mStop) {
			if (!BluetoothController.isConnectedToServer)
				try {
					// Connect the device through the socket. This will block
					// until it succeeds or throws an exception, which can happen
					// if it doesn't find anything to connect to within about 4 seconds.
					Logger.logComms { "Attempting to connect to a Bluetooth server on '${mDevice.name}'." }
					mDevice.createRfcommSocketToServiceRecord(mUUID)?.also {
						it.connect()
						// If the previous line didn't throw an IOException, then it connected OK.
						// Do work to manage the connection (in a separate thread)
						Logger.logComms { "Connected to a Bluetooth server on '${mDevice.name}'." }
						mmSocket = it
						mOnConnectedFunction(it)

					}
				} catch (se: SecurityException) {
					Logger.logComms(
						"A Bluetooth security exception was thrown, despite the controller being connected to the server.",
						se
					)
				} catch (connectException: Exception) {
					// There probably isn't a server to connect to. Wait a bit and try again.
					Logger.logComms { "Failed to connect to a server on '${mDevice.name}'." }
					Utils.safeThreadWait(1000)
				}
			else {
				// Already connected. Wait a bit and try/check again.
				Utils.safeThreadWait(2000)
			}
		}
	}

	/**
	 *  Will cancel an in-progress connection, and close the socket
	 */
	internal fun stopTrying() {
		mStop = true
		closeSocket()
	}

	/**
	 * Closes the watching socket.
	 */
	private fun closeSocket() {
		try {
			Logger.logComms("Closing the server searching socket.")
			mmSocket?.close()
			Logger.logComms("Closed the server searching socket.")
		} catch (e: IOException) {
			Logger.logComms("Error closing Bluetooth socket.", e)
		}
	}
}