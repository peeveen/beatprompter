package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import java.io.IOException
import java.util.UUID

/**
 * This class is a thread that runs when the app is in BlueToothServer mode. It listens for connections from
 * paired clients, and creates an output socket from each connection. Any events broadcast from this instance
 * of the app will be sent to all output sockets.
 */
class ServerThread internal constructor(
	private val bluetoothAdapter: BluetoothAdapter,
	private val uuid: UUID,
	private val onConnectedFunction: (socket: BluetoothSocket) -> Unit
) : Thread() {
	private var serverSocket: BluetoothServerSocket? = null
	private var stop = false
	private val socketNullLock = Any()

	override fun run() {
		// Keep listening until exception occurs or a socket is returned
		while (!stop) {
			try {
				val serverSocket = synchronized(socketNullLock) {
					serverSocket ?: run {
						try {
							// MY_UUID is the app's UUID string, also used by the server code
							serverSocket =
								bluetoothAdapter.listenUsingRfcommWithServiceRecord(BeatPrompter.APP_NAME, uuid)
							Logger.logComms("Created the Bluetooth server socket.")
						} catch (se: SecurityException) {
							Logger.logComms(
								"A Bluetooth security exception was thrown, despite the server socket already having been created.",
								se
							)
						} catch (e: IOException) {
							Logger.logComms("Error creating Bluetooth server socket.", e)
						}
						serverSocket
					}
				}

				// If a connection was accepted
				// Do work to manage the connection (in a separate thread)
				Logger.logComms("Looking for a client connection.")
				serverSocket?.accept(5000)?.also {
					Logger.logComms("Found a client connection.")
					onConnectedFunction(it)
				}
			} catch (e: Exception) {
				//Log.e(BLUETOOTH_TAG, "Failed to accept new Bluetooth connection.",e);
			}
		}
	}

	/** Will cancel the listening socket, and cause the thread to finish  */
	fun stopListening() {
		stop = true
		synchronized(socketNullLock) {
			try {
				Logger.logComms("Closing Bluetooth server socket.")
				serverSocket?.close()
				Logger.logComms("Closed Bluetooth server socket.")
			} catch (e: IOException) {
				Logger.logComms("Failed to close Bluetooth listener socket.", e)
			} finally {
				serverSocket = null
			}
		}
	}
}
