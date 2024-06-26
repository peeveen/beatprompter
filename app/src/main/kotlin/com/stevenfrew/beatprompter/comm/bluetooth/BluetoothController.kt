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
import android.os.Build
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.EventRouter
import com.stevenfrew.beatprompter.Events
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.MessageQueue
import com.stevenfrew.beatprompter.comm.OutgoingMessage
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.comm.bluetooth.message.HeartbeatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * General Bluetooth management singleton object.
 */
object BluetoothController : SharedPreferences.OnSharedPreferenceChangeListener, CoroutineScope {
	private val mCoRoutineJob = Job()
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Default + mCoRoutineJob

	private var mInitialised = false

	// Our unique app Bluetooth ID.
	private val BLUETOOTH_UUID = UUID(0x49ED8190882ADC90L, -0x6c036df6ed2c22d2L)

	// The device Bluetooth adapter, if one exists.
	private var mBluetoothAdapter: BluetoothAdapter? = null

	private const val BLUETOOTH_QUEUE_SIZE = 4096
	private val mBluetoothOutQueue = MessageQueue(BLUETOOTH_QUEUE_SIZE)
	private val mSenderTask = SenderTask(mBluetoothOutQueue)
	private val mReceiverTasks = ReceiverTasks()

	private val mSenderTaskThread = Thread(mSenderTask)

	// Threads that watch for client/server connections, and an object to synchronize their
	// use.
	private val mBluetoothThreadsLock = Any()
	private var mServerBluetoothThread: ServerThread? = null
	private var mConnectToServerThread: ConnectToServerThread? = null

	/**
	 * Called when the app starts. Doing basic Bluetooth setup.
	 */
	fun initialise(application: BeatPrompter) {
		val bluetoothManager =
			application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		mBluetoothAdapter = bluetoothManager.adapter

		if (mBluetoothAdapter != null) {
			Logger.logComms("Bluetooth adapter found.")
			Logger.logComms("Starting Bluetooth sender thread.")
			mSenderTaskThread.start()
			Task.resumeTask(mSenderTask)
			Logger.logComms("Bluetooth sender thread started.")

			launch {
				while (true) {
					mBluetoothOutQueue.putMessage(HeartbeatMessage)
					delay(1000)
				}
			}

			application.apply {
				registerReceiver(mAdapterReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
				registerReceiver(mDeviceReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
			}
			Preferences.registerOnSharedPreferenceChangeListener(this)
			onBluetoothActivation()
			mInitialised = true
		}
	}

	/**
	 * User could switch Bluetooth functionality on/off at any time.
	 * We need to keep an eye on that.
	 */
	private val mAdapterReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
				when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
					BluetoothAdapter.STATE_TURNING_OFF -> onStopBluetooth()
					BluetoothAdapter.STATE_ON -> onBluetoothActivation()
				}
			}
		}
	}

	private fun onBluetoothActivation() {
		Logger.logComms("Bluetooth is on.")
		if (Preferences.bluetoothMode !== BluetoothMode.None)
			onStartBluetooth()
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
	 * Do we have a connection to a band leader?
	 */
	val isConnectedToServer: Boolean
		get() = mReceiverTasks.taskCount > 0

	/**
	 * As a band leader, how many band members are we connected to?
	 */
	val bluetoothClientCount: Int
		get() = mSenderTask.senderCount

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
	private fun onStartBluetooth() {
		if (mBluetoothAdapter != null)
			startBluetoothWatcherThreads()
	}

	/**
	 * Starts up all the Bluetooth connection-watcher threads.
	 */
	private fun startBluetoothWatcherThreads() {
		if (mBluetoothAdapter?.isEnabled == true) {
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
								ServerThread(mBluetoothAdapter!!, BLUETOOTH_UUID) { socket ->
									handleConnectionFromClient(socket)
								}.apply { start() }
						}
					}

					else -> {
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
		if (key == BeatPrompter.getResourceString(R.string.pref_bluetoothMode_key)) {
			Logger.logComms("Bluetooth mode changed.")
			if (Preferences.bluetoothMode === BluetoothMode.None)
				onStopBluetooth()
			else
				onStartBluetooth()
		} else if (key == BeatPrompter.getResourceString(R.string.pref_bandLeaderDevice_key)) {
			Logger.logComms("Band leader device changed.")
			if (Preferences.bluetoothMode === BluetoothMode.Client) {
				shutDownBluetoothClient()
				startBluetoothWatcherThreads()
			}
		}
	}

	fun removeReceiver(task: ReceiverTask) {
		mReceiverTasks.stopAndRemoveReceiver(task.mName)
	}

	internal fun putMessage(message: OutgoingMessage) {
		if (mInitialised)
			mBluetoothOutQueue.putMessage(message)
	}
}
