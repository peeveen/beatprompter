package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.MessageQueue
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.comm.bluetooth.message.HeartbeatMessage
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * General Bluetooth management singleton object.
 */
object BluetoothManager : SharedPreferences.OnSharedPreferenceChangeListener, CoroutineScope {
    private val mCoRoutineJob = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + mCoRoutineJob

    // Our unique app Bluetooth ID.
    private val BLUETOOTH_UUID = UUID(0x49ED8190882ADC90L, -0x6c036df6ed2c22d2L)

    // The device Bluetooth adapter, if one exists.
    private var mBluetoothAdapter: BluetoothAdapter? = null

    private const val BLUETOOTH_QUEUE_SIZE = 4096
    val mBluetoothOutQueue = MessageQueue(BLUETOOTH_QUEUE_SIZE)
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
    fun initialise(application: BeatPrompterApplication) {
        Log.d(BeatPrompterApplication.TAG_COMMS, "Starting Bluetooth sender thread.")
        mSenderTaskThread.start()
        Task.resumeTask(mSenderTask)
        Log.d(BeatPrompterApplication.TAG_COMMS, "Bluetooth sender thread started.")

        launch {
            while (true) {
                BluetoothManager.mBluetoothOutQueue.putMessage(HeartbeatMessage)
                delay(1000)
            }
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (mBluetoothAdapter != null) {
            Log.d(BeatPrompterApplication.TAG_COMMS, "Bluetooth adapter found.")
            application.registerReceiver(mAdapterReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            application.registerReceiver(mDeviceReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
            BeatPrompterApplication.preferences.registerOnSharedPreferenceChangeListener(this)
            onBluetoothActivation()
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
                    BluetoothAdapter.STATE_TURNING_OFF -> BluetoothManager.onStopBluetooth()
                    BluetoothAdapter.STATE_ON -> onBluetoothActivation()
                }
            }
        }
    }

    private fun onBluetoothActivation() {
        Log.d(BeatPrompterApplication.TAG_COMMS, "Bluetooth is on.")
        val bluetoothMode = bluetoothMode
        if (bluetoothMode !== BluetoothMode.None)
            BluetoothManager.onStartBluetooth()
    }

    private val mDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                // Something has disconnected.
                (intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice).apply {
                    Log.d(BeatPrompterApplication.TAG_COMMS, "A Bluetooth device with address '$address' has disconnected.")
                    mReceiverTasks.stopAndRemoveReceiver(address)
                    mSenderTask.removeSender(address)
                }
            }
        }
    }

    /**
     * The app can run in three Bluetooth modes:
     * - Server (it is listening for connections from other band members)
     * - Client (it is looking for the band leader to hear events from)
     * - None (not listening)
     * This returns the current mode.
     */
    val bluetoothMode: BluetoothMode
        get() {
            return try {
                BluetoothMode.valueOf(BeatPrompterApplication.preferences.getString(BeatPrompterApplication.getResourceString(R.string.pref_bluetoothMode_key),
                        BeatPrompterApplication.getResourceString(R.string.bluetoothModeNoneValue))!!)
            } catch (e: Exception) {
                // Backwards compatibility with old shite values from previous app versions.
                BluetoothMode.None
            }
        }

    /**
     * The app can run in three Bluetooth modes:
     * - Server (it is listening for connections from other band members)
     * - Client (it is looking for the band leader to hear events from)
     * - None (not listening)
     * This returns the current mode.
     */
    private val bandLeaderAddress: String
        get() = BeatPrompterApplication.preferences.getString(BeatPrompterApplication.getResourceString(R.string.pref_bandLeaderDevice_key), "")!!

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
        Log.d(BeatPrompterApplication.TAG_COMMS, "Bluetooth has stopped.")
        shutDownBluetoothServer()
        shutDownBluetoothClient()
    }

    /**
     * Shuts down the Bluetooth server, stops the server thread, and disconnects all connected clients.
     */
    private fun shutDownBluetoothServer() {
        mSenderTask.removeAll()
        Log.d(BeatPrompterApplication.TAG_COMMS, "Shutting down the Bluetooth server thread.")
        synchronized(mBluetoothThreadsLock) {
            mServerBluetoothThread?.run {
                try {
                    Log.d(BeatPrompterApplication.TAG_COMMS, "Stopping listening on Bluetooth server thread.")
                    stopListening()
                    Log.d(BeatPrompterApplication.TAG_COMMS, "Interrupting Bluetooth server thread.")
                    interrupt()
                    Log.d(BeatPrompterApplication.TAG_COMMS, "Joining Bluetooth server thread.")
                    join()
                    Log.d(BeatPrompterApplication.TAG_COMMS, "Bluetooth server thread now finished.")
                } catch (e: Exception) {
                    Log.e(BeatPrompterApplication.TAG_COMMS, "Error stopping BlueTooth server connection accepting thread, on thread join.", e)
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
        Log.d(BeatPrompterApplication.TAG_COMMS, "Shutting down the Bluetooth client threads.")
        synchronized(mBluetoothThreadsLock) {
            if (mConnectToServerThread != null)
                try {
                    with(mConnectToServerThread!!)
                    {
                        Log.d(BeatPrompterApplication.TAG_COMMS, "Stopping listening on a Bluetooth client thread.")
                        stopTrying()
                        Log.d(BeatPrompterApplication.TAG_COMMS, "Interrupting a Bluetooth client thread.")
                        interrupt()
                        Log.d(BeatPrompterApplication.TAG_COMMS, "Joining a Bluetooth client thread.")
                        join()
                        Log.d(BeatPrompterApplication.TAG_COMMS, "A Bluetooth client thread has now finished.")
                    }
                } catch (e: Exception) {
                    Log.e(BeatPrompterApplication.TAG_COMMS, "Error stopping BlueTooth client connection thread, on thread join.", e)
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
        if (mBluetoothAdapter != null && mBluetoothAdapter!!.isEnabled) {
            synchronized(mBluetoothThreadsLock) {
                val mode = bluetoothMode
                if (mode === BluetoothMode.Server) {
                    shutDownBluetoothClient()
                    if (mServerBluetoothThread == null) {
                        Log.d(BeatPrompterApplication.TAG_COMMS, "Starting Bluetooth server thread.")
                        mServerBluetoothThread = ServerThread(mBluetoothAdapter!!, BLUETOOTH_UUID) { socket -> handleConnectionFromClient(socket) }.apply { start() }
                    }
                } else if (mode === BluetoothMode.Client) {
                    shutDownBluetoothServer()
                    if (mConnectToServerThread == null) {
                        mBluetoothAdapter!!.bondedDevices.firstOrNull { it.address == bandLeaderAddress }?.also {
                            try {
                                Log.d(BeatPrompterApplication.TAG_COMMS, "Starting Bluetooth client thread, looking to connect with '${it.name}'.")
                                mConnectToServerThread = ConnectToServerThread(it, BLUETOOTH_UUID) { socket -> BluetoothManager.setServerConnection(socket) }.apply { start() }
                            } catch (e: Exception) {
                                Log.e(BeatPrompterApplication.TAG_COMMS, "Failed to create ConnectToServerThread for bluetooth device ${it.name}'.", e)
                            }
                        }
                    }
                }
            }
        }
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return mBluetoothAdapter?.bondedDevices?.toList() ?: listOf()
    }

    /**
     * Adds a new connection to the pool of connected clients, and informs the user about the
     * new connection.
     */
    private fun handleConnectionFromClient(socket: BluetoothSocket) {
        if (bluetoothMode === BluetoothMode.Server) {
            Log.d(BeatPrompterApplication.TAG_COMMS, "Client connection opened with '${socket.remoteDevice.name}'")
            mSenderTask.addSender(socket.remoteDevice.address, Sender(socket))
            EventHandler.sendEventToSongList(EventHandler.CONNECTION_ADDED, socket.remoteDevice.name)
        }
    }

    /**
     * Sets the server connection socket once we connect.
     */
    private fun setServerConnection(socket: BluetoothSocket) {
        if (bluetoothMode === BluetoothMode.Client) {
            Log.d(BeatPrompterApplication.TAG_COMMS, "Server connection opened with '${socket.remoteDevice.name}'")
            mReceiverTasks.addReceiver(socket.remoteDevice.address, socket.remoteDevice.name, Receiver(socket))
            EventHandler.sendEventToSongList(EventHandler.CONNECTION_ADDED, socket.remoteDevice.name)
        }
    }

    /**
     * Called when the user changes pertinent Bluetooth preferences.
     */
    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        if (key == BeatPrompterApplication.getResourceString(R.string.pref_bluetoothMode_key)) {
            Log.d(BeatPrompterApplication.TAG_COMMS, "Bluetooth mode changed.")
            val mode = bluetoothMode
            if (mode === BluetoothMode.None)
                onStopBluetooth()
            else
                onStartBluetooth()
        } else if (key == BeatPrompterApplication.getResourceString(R.string.pref_bandLeaderDevice_key)) {
            Log.d(BeatPrompterApplication.TAG_COMMS, "Band leader device changed.")
            val mode = bluetoothMode
            if (mode === BluetoothMode.Client) {
                shutDownBluetoothClient()
                startBluetoothWatcherThreads()
            }
        }
    }

    fun removeReceiver(task: ReceiverTask) {
        mReceiverTasks.stopAndRemoveReceiver(task.mName)
    }
}
