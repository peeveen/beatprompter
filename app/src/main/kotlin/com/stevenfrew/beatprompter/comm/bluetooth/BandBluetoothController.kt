package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
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

object BandBluetoothController : CoroutineScope {
	// Due to this nonsense:
	// https://developer.android.com/reference/android/content/SharedPreferences.html#registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener)
	// we have to keep a reference to the prefs listener, or it gets garbage collected.
	private var mPrefsListener: OnSharedPreferenceChangeListener? = null

	private const val BLUETOOTH_COMM_TYPE = "Bluetooth"

	// Our unique controller Bluetooth ID.
	private val BAND_BLUETOOTH_UUID = UUID(0x49ED8190882ADC90L, -0x6c036df6ed2c22d2L)
	private val mCoRoutineJob = Job()
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Default + mCoRoutineJob

	// Threads that watch for client/server connections, and an object to synchronize their
	// use.
	private val mBluetoothThreadsLock = Any()
	private var mServerBluetoothThread: ServerThread? = null
	private var mConnectToBandLeaderThread: ConnectToServerThread? = null
	private var mBluetoothAdapter: BluetoothAdapter? = null

	fun initialize(
		context: Context,
		senderTask: SenderTask,
		receiverTasks: ReceiverTasks
	) {
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
						override fun onBluetoothDisabled() = onStopBluetooth(senderTask, receiverTasks)
						override fun onBluetoothEnabled(context: Context) =
							onBluetoothActivation(context, it, senderTask, receiverTasks)
					},
					IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
				)
				registerReceiver(
					DeviceReceiver(senderTask, receiverTasks),
					IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
				)
			}
			val prefsListener = OnSharedPreferenceChangeListener { _, key ->
				val bluetoothModeKey =
					BeatPrompter.appResources.getString(R.string.pref_bluetoothMode_key)
				val bandLeaderDeviceKey =
					BeatPrompter.appResources.getString(R.string.pref_bandLeaderDevice_key)
				when (key) {
					bluetoothModeKey -> {
						Logger.logComms("Bluetooth mode changed.")
						if (Preferences.bluetoothMode === BluetoothMode.None)
							onStopBluetooth(senderTask, receiverTasks)
						else
							onStartBluetooth(context, it, senderTask, receiverTasks)
					}

					bandLeaderDeviceKey -> {
						Logger.logComms("Band leader device changed.")
						if (Preferences.bluetoothMode === BluetoothMode.Client) {
							shutDownBluetoothClient(receiverTasks)
							startBluetoothWatcherThreads(context, it, senderTask, receiverTasks)
						}
					}
				}
			}
			Preferences.registerOnSharedPreferenceChangeListener(prefsListener)
			mPrefsListener = prefsListener

			onBluetoothActivation(context, it, senderTask, receiverTasks)

			launch {
				while (true) {
					Bluetooth.putMessage(HeartbeatMessage)
					delay(1000)
				}
			}
		}
	}

	private fun onBluetoothActivation(
		context: Context,
		bluetoothAdapter: BluetoothAdapter,
		senderTask: SenderTask,
		receiverTasks: ReceiverTasks
	) {
		Logger.logComms("Bluetooth is on.")
		if (Preferences.bluetoothMode !== BluetoothMode.None)
			onStartBluetooth(context, bluetoothAdapter, senderTask, receiverTasks)
	}

	/**
	 * Called when Bluetooth is switched off.
	 */
	private fun onStopBluetooth(senderTask: SenderTask, receiverTasks: ReceiverTasks) {
		Logger.logComms("Bluetooth has stopped.")
		shutDownBluetoothServer(senderTask)
		shutDownBluetoothClient(receiverTasks)
	}

	/**
	 * Shuts down the Bluetooth server, stops the server thread, and disconnects all connected clients.
	 */
	private fun shutDownBluetoothServer(senderTask: SenderTask) {
		senderTask.removeAll(BLUETOOTH_COMM_TYPE)
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
	private fun shutDownBluetoothClient(receiverTasks: ReceiverTasks) {
		receiverTasks.stopAndRemoveAll(BLUETOOTH_COMM_TYPE)
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
	private fun onStartBluetooth(
		context: Context,
		bluetoothAdapter: BluetoothAdapter,
		senderTask: SenderTask,
		receiverTasks: ReceiverTasks
	) =
		startBluetoothWatcherThreads(context, bluetoothAdapter, senderTask, receiverTasks)

	/**
	 * Starts up all the Bluetooth connection-watcher threads.
	 */
	private fun startBluetoothWatcherThreads(
		context: Context,
		bluetoothAdapter: BluetoothAdapter,
		senderTask: SenderTask,
		receiverTasks: ReceiverTasks
	) {
		if (bluetoothAdapter.isEnabled) {
			synchronized(mBluetoothThreadsLock) {
				when (Preferences.bluetoothMode) {
					BluetoothMode.Client -> {
						shutDownBluetoothServer(senderTask)
						if (mConnectToBandLeaderThread == null) {
							Bluetooth.getPairedDevices(context)
								.firstOrNull { it.address == Preferences.bandLeaderDevice }
								?.also {
									try {
										Logger.logComms { "Starting Bluetooth client thread, looking to connect with '${it.name}'." }
										mConnectToBandLeaderThread =
											ConnectToServerThread(it, BAND_BLUETOOTH_UUID) { socket ->
												setServerConnection(socket, receiverTasks)
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
						shutDownBluetoothClient(receiverTasks)
						if (mServerBluetoothThread == null) {
							Logger.logComms("Starting Bluetooth server thread.")
							mServerBluetoothThread =
								ServerThread(bluetoothAdapter, BAND_BLUETOOTH_UUID) { socket ->
									handleConnectionFromClient(socket, senderTask)
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
	private fun handleConnectionFromClient(socket: BluetoothSocket, senderTask: SenderTask) {
		if (Preferences.bluetoothMode === BluetoothMode.Server)
			try {
				Logger.logComms { "Client connection opened with '${socket.remoteDevice.name}'" }
				senderTask.addSender(socket.remoteDevice.address, Sender(socket, BLUETOOTH_COMM_TYPE))
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
	private fun setServerConnection(socket: BluetoothSocket, receiverTasks: ReceiverTasks) {
		try {
			if (Preferences.bluetoothMode === BluetoothMode.Client) {
				Logger.logComms { "Server connection opened with '${socket.remoteDevice.name}'" }
				receiverTasks.addReceiver(
					socket.remoteDevice.address,
					socket.remoteDevice.name,
					Receiver(socket, BLUETOOTH_COMM_TYPE)
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
}