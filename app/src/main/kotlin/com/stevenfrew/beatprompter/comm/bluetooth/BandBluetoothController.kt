package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.comm.bluetooth.message.HeartbeatMessage
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class BandBluetoothController(
	context: Context,
	private val mSenderTask: SenderTask,
	private val mReceiverTasks: ReceiverTasks
) : CoroutineScope {
	private val mCoRoutineJob = Job()
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Default + mCoRoutineJob

	// Threads that watch for client/server connections, and an object to synchronize their
	// use.
	private val mBluetoothThreadsLock = Any()
	private var mServerBluetoothThread: ServerThread? = null
	private var mConnectToBandLeaderThread: ConnectToServerThread? = null
	private var mBluetoothAdapter: BluetoothAdapter? = null

	init {
		Bluetooth.getBluetoothAdapter(context)?.also {
			mBluetoothAdapter = it
			Logger.logComms("Bluetooth adapter found.")
			Logger.logComms("Starting BandBluetooth sender thread.")
			Logger.logComms("BandBluetooth sender thread started.")

			context.apply {
				registerReceiver(
					/**
					 * User could switch Bluetooth functionality on/off at any time.
					 * We need to keep an eye on that.
					 */
					object : AdapterReceiver() {
						override fun onBluetoothDisabled() = onStopBluetooth()
						override fun onBluetoothEnabled(context: Context) = onBluetoothActivation(context, it)
					},
					IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
				)
				registerReceiver(
					DeviceReceiver(mSenderTask, mReceiverTasks),
					IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
				)
			}
			Preferences.registerOnSharedPreferenceChangeListener { _, key ->
				val bluetoothModeKey =
					BeatPrompter.appResources.getString(R.string.pref_bluetoothMode_key)
				val bandLeaderDeviceKey =
					BeatPrompter.appResources.getString(R.string.pref_bandLeaderDevice_key)
				when (key) {
					bluetoothModeKey -> {
						Logger.logComms("Bluetooth mode changed.")
						if (Preferences.bluetoothMode === BluetoothMode.None)
							onStopBluetooth()
						else
							onStartBluetooth(context, it)
					}

					bandLeaderDeviceKey -> {
						Logger.logComms("Band leader device changed.")
						if (Preferences.bluetoothMode === BluetoothMode.Client) {
							shutDownBluetoothClient()
							startBluetoothWatcherThreads(context, it)
						}
					}
				}
			}
			onBluetoothActivation(context, it)

			launch {
				while (true) {
					Bluetooth.putMessage(HeartbeatMessage)
					delay(1000)
				}
			}
		}
	}

	private fun onBluetoothActivation(context: Context, bluetoothAdapter: BluetoothAdapter) {
		Logger.logComms("Bluetooth is on.")
		if (Preferences.bluetoothMode !== BluetoothMode.None)
			onStartBluetooth(context, bluetoothAdapter)
	}

	/**
	 * Called when Bluetooth is switched off.
	 */
	private fun onStopBluetooth() {
		Logger.logComms("Bluetooth has stopped.")
		shutDownBluetoothServer()
		shutDownBluetoothClient()
	}

	/**
	 * Shuts down the Bluetooth server, stops the server thread, and disconnects all connected clients.
	 */
	private fun shutDownBluetoothServer() {
		mSenderTask.removeAll()
		Logger.logComms("Shutting down the Bluetooth server thread.")
		synchronized(mBluetoothThreadsLock) {
			mServerBluetoothThread?.run {
				try {
					Logger.logComms("Stopping listening on Bluetooth server thread.")
					stopListening()
					Logger.logComms("Interrupting Bluetooth server thread.")
					interrupt()
					Logger.logComms("Joining Bluetooth server thread.")
					join()
					Logger.logComms("Bluetooth server thread now finished.")
				} catch (e: Exception) {
					Logger.logComms(
						"Error stopping BlueTooth server connection accepting thread, on thread join.",
						e
					)
				} finally {
					mServerBluetoothThread = null
				}
			}
		}
	}

	/**
	 * Shuts down the Bluetooth client, stops the client connection thread, and disconnects from
	 * the server.
	 */
	private fun shutDownBluetoothClient() {
		mReceiverTasks.stopAll()
		Logger.logComms("Shutting down the Bluetooth client threads.")
		synchronized(mBluetoothThreadsLock) {
			if (mConnectToBandLeaderThread != null)
				try {
					mConnectToBandLeaderThread?.apply {
						Logger.logComms("Stopping listening on a Bluetooth client thread.")
						stopTrying()
						Logger.logComms("Interrupting a Bluetooth client thread.")
						interrupt()
						Logger.logComms("Joining a Bluetooth client thread.")
						join()
						Logger.logComms("A Bluetooth client thread has now finished.")
					}
				} catch (e: Exception) {
					Logger.logComms("Error stopping BlueTooth client connection thread, on thread join.", e)
				}
			mConnectToBandLeaderThread = null
		}
	}

	/**
	 * Called when Bluetooth functionality is switched on.
	 */
	private fun onStartBluetooth(context: Context, bluetoothAdapter: BluetoothAdapter) =
		startBluetoothWatcherThreads(context, bluetoothAdapter)

	/**
	 * Starts up all the Bluetooth connection-watcher threads.
	 */
	private fun startBluetoothWatcherThreads(context: Context, bluetoothAdapter: BluetoothAdapter) {
		if (bluetoothAdapter.isEnabled) {
			synchronized(mBluetoothThreadsLock) {
				when (Preferences.bluetoothMode) {
					BluetoothMode.Client -> {
						shutDownBluetoothServer()
						if (mConnectToBandLeaderThread == null) {
							Bluetooth.getPairedDevices(context)
								.firstOrNull { it.address == Preferences.bandLeaderDevice }
								?.also {
									try {
										Logger.logComms { "Starting Bluetooth client thread, looking to connect with '${it.name}'." }
										mConnectToBandLeaderThread =
											ConnectToServerThread(it, BAND_BLUETOOTH_UUID) { socket ->
												setServerConnection(socket)
											}.apply { start() }
									} catch (se: SecurityException) {
										Logger.logComms(
											"Bluetooth security exception was thrown, despite adapter being enabled.",
											se
										)
									} catch (e: Exception) {
										Logger.logComms(
											{ "Failed to create ConnectToServerThread for bluetooth device ${it.name}'." },
											e
										)
									}
								}
						}
					}

					BluetoothMode.Server -> {
						shutDownBluetoothClient()
						if (mServerBluetoothThread == null) {
							Logger.logComms("Starting Bluetooth server thread.")
							mServerBluetoothThread =
								ServerThread(bluetoothAdapter, BAND_BLUETOOTH_UUID) { socket ->
									handleConnectionFromClient(socket)
								}.apply { start() }
						}
					}

					BluetoothMode.None -> {
						// Don't do anything!
					}
				}
			}
		}
	}

	/**
	 * Adds a new connection to the pool of connected clients, and informs the user about the
	 * new connection.
	 */
	private fun handleConnectionFromClient(socket: BluetoothSocket) {
		if (Preferences.bluetoothMode === BluetoothMode.Server)
			try {
				Logger.logComms { "Client connection opened with '${socket.remoteDevice.name}'" }
				mSenderTask.addSender(socket.remoteDevice.address, Sender(socket))
				EventRouter.sendEventToSongList(Events.CONNECTION_ADDED, socket.remoteDevice.name)
			} catch (se: SecurityException) {
				Logger.logComms(
					"A Bluetooth security exception was thrown while handling connection from client.",
					se
				)
			}
	}

	/**
	 * Sets the server connection socket once we connect.
	 */
	private fun setServerConnection(socket: BluetoothSocket) {
		try {
			if (Preferences.bluetoothMode === BluetoothMode.Client) {
				Logger.logComms { "Server connection opened with '${socket.remoteDevice.name}'" }
				mReceiverTasks.addReceiver(
					socket.remoteDevice.address,
					socket.remoteDevice.name,
					Receiver(socket)
				)
				EventRouter.sendEventToSongList(Events.CONNECTION_ADDED, socket.remoteDevice.name)
			}
		} catch (se: SecurityException) {
			Logger.logComms(
				"A Bluetooth security exception was thrown while creating a server connection.",
				se
			)
		}
	}

	companion object {
		// Our unique controller Bluetooth ID.
		private val BAND_BLUETOOTH_UUID = UUID(0x49ED8190882ADC90L, -0x6c036df6ed2c22d2L)
	}
}