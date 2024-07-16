package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import java.util.UUID

class BluetoothController(
	context: Context,
	internal val mSenderTask: SenderTask,
	internal val mReceiverTasks: ReceiverTasks
) : SharedPreferences.OnSharedPreferenceChangeListener {
	// Threads that watch for client/server connections, and an object to synchronize their
	// use.
	private val mBluetoothThreadsLock = Any()
	private var mServerBluetoothThread: ServerThread? = null
	private var mConnectToServerThread: ConnectToServerThread? = null
	private var mBluetoothAdapter: BluetoothAdapter? = null

	internal val isActive: Boolean
		get() = mBluetoothAdapter != null

	init {
		getBluetoothAdapter(context)?.also {
			mBluetoothAdapter = it
			Logger.logComms("Bluetooth adapter found.")
			Logger.logComms("Starting Bluetooth sender thread.")
			Logger.logComms("Bluetooth sender thread started.")

			context.apply {
				registerReceiver(
					AdapterReceiver(it),
					IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
				)
				registerReceiver(mDeviceReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
			}
			Preferences.registerOnSharedPreferenceChangeListener(this)
			onBluetoothActivation(it)
		}
	}

	/**
	 * User could switch Bluetooth functionality on/off at any time.
	 * We need to keep an eye on that.
	 */
	private inner class AdapterReceiver(private val mBluetoothAdapter: BluetoothAdapter) :
		BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
				when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
					BluetoothAdapter.STATE_TURNING_OFF -> onStopBluetooth()
					BluetoothAdapter.STATE_ON -> onBluetoothActivation(mBluetoothAdapter)
				}
			}
		}
	}

	private fun onBluetoothActivation(bluetoothAdapter: BluetoothAdapter) {
		Logger.logComms("Bluetooth is on.")
		if (Preferences.bluetoothMode !== BluetoothMode.None)
			onStartBluetooth(bluetoothAdapter)
	}

	private val mDeviceReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
				// Something has disconnected.
				(if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
					intent.getParcelableExtra(
						BluetoothDevice.EXTRA_DEVICE
					)
				else
					intent.getParcelableExtra(
						BluetoothDevice.EXTRA_DEVICE,
						BluetoothDevice::class.java
					))?.apply {
					Logger.logComms { "A Bluetooth device with address '$address' has disconnected." }
					mReceiverTasks.stopAndRemoveReceiver(address)
					mSenderTask.removeSender(address)
				}
			}
		}
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
			if (mConnectToServerThread != null)
				try {
					with(mConnectToServerThread!!)
					{
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
			mConnectToServerThread = null
		}
	}

	/**
	 * Called when Bluetooth functionality is switched on.
	 */
	private fun onStartBluetooth(bluetoothAdapter: BluetoothAdapter) {
		startBluetoothWatcherThreads(bluetoothAdapter)
	}

	/**
	 * Starts up all the Bluetooth connection-watcher threads.
	 */
	private fun startBluetoothWatcherThreads(bluetoothAdapter: BluetoothAdapter) {
		if (bluetoothAdapter.isEnabled) {
			synchronized(mBluetoothThreadsLock) {
				when (Preferences.bluetoothMode) {
					BluetoothMode.Client -> {
						shutDownBluetoothServer()
						if (mConnectToServerThread == null) {
							getPairedDevices()
								.firstOrNull { it.address == Preferences.bandLeaderDevice }
								?.also {
									try {
										Logger.logComms { "Starting Bluetooth client thread, looking to connect with '${it.name}'." }
										mConnectToServerThread =
											ConnectToServerThread(it, BLUETOOTH_UUID) { socket ->
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
								ServerThread(bluetoothAdapter, BLUETOOTH_UUID) { socket ->
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

	fun getPairedDevices(): List<BluetoothDevice> {
		return try {
			mBluetoothAdapter?.bondedDevices?.toList() ?: listOf()
		} catch (se: SecurityException) {
			Logger.logComms("A Bluetooth security exception was thrown while getting paired devices.", se)
			listOf()
		}
	}

	/**
	 * Adds a new connection to the pool of connected clients, and informs the user about the
	 * new connection.
	 */
	private fun handleConnectionFromClient(socket: BluetoothSocket) {
		try {
			if (Preferences.bluetoothMode === BluetoothMode.Server) {
				Logger.logComms { "Client connection opened with '${socket.remoteDevice.name}'" }
				mSenderTask.addSender(socket.remoteDevice.address, Sender(socket))
				EventRouter.sendEventToSongList(Events.CONNECTION_ADDED, socket.remoteDevice.name)
			}
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

	/**
	 * Called when the user changes pertinent Bluetooth preferences.
	 */
	override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
		val bluetoothModeKey = BeatPrompter.appResources.getString(R.string.pref_bluetoothMode_key)
		val bandLeaderDeviceKey =
			BeatPrompter.appResources.getString(R.string.pref_bandLeaderDevice_key)
		when (key) {
			bluetoothModeKey, bandLeaderDeviceKey -> {
				mBluetoothAdapter?.also {
					when (key) {
						bluetoothModeKey -> {
							Logger.logComms("Bluetooth mode changed.")
							if (Preferences.bluetoothMode === BluetoothMode.None)
								onStopBluetooth()
							else
								onStartBluetooth(it)
						}

						bandLeaderDeviceKey -> {
							Logger.logComms("Band leader device changed.")
							if (Preferences.bluetoothMode === BluetoothMode.Client) {
								shutDownBluetoothClient()
								startBluetoothWatcherThreads(it)
							}
						}
					}
				}
			}
		}
	}

	private fun getBluetoothAdapter(context: Context): BluetoothAdapter? {
		return if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
			val bluetoothManager =
				context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
			bluetoothManager.adapter
		} else null
	}

	companion object {
		// Our unique app Bluetooth ID.
		private val BLUETOOTH_UUID = UUID(0x49ED8190882ADC90L, -0x6c036df6ed2c22d2L)
	}
}