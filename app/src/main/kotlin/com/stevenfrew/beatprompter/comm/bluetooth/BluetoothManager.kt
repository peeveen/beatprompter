package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.*
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.comm.OutgoingMessage
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.comm.bluetooth.message.ChooseSongMessage
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

/**
 * General Bluetooth management singleton object.
 */
object BluetoothManager:SharedPreferences.OnSharedPreferenceChangeListener {
    // Logging tag
    const val BLUETOOTH_TAG = "bpbt"

    // Our unique app Bluetooth ID.
    val BLUETOOTH_UUID = UUID(0x49ED8190882ADC90L, -0x6c036df6ed2c22d2L)

    // The device Bluetooth adapter, if one exists.
    private var mBluetoothAdapter: BluetoothAdapter? = null

    private const val BLUETOOTH_QUEUE_SIZE = 1024
    var mBluetoothOutQueue = ArrayBlockingQueue<OutgoingMessage>(BLUETOOTH_QUEUE_SIZE)
    private val mSenderTask= SenderTask(mBluetoothOutQueue)
    private val mReceiverTask= ReceiverTask()

    // Threads that watch for client/server connections, and an object to synchronize their
    // use.
    private val mBluetoothThreadsLock = Any()
    private var mServerBluetoothThread: ServerThread? = null
    private val mConnectToServerThreads = mutableListOf<ConnectToServerThread>()

    /**
     * Called when the app starts. Doing basic Bluetooth setup.
     */
    fun initialise(application: BeatPrompterApplication) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if(mBluetoothAdapter!=null) {
            application.registerReceiver(mAdapterReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            BeatPrompterApplication.preferences.registerOnSharedPreferenceChangeListener(this)

            val bluetoothMode = bluetoothMode
            if (bluetoothMode !== BluetoothMode.None)
                onStartBluetooth(bluetoothMode)
        }
    }

    /**
     * User could switch Bluetooth functionality on/off at any time.
     * We need to keep an eye on that.
     */
    private val mAdapterReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action==BluetoothAdapter.ACTION_STATE_CHANGED)
            {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> BluetoothManager.onStopBluetooth()
                    BluetoothAdapter.STATE_ON -> {
                        val bluetoothMode = bluetoothMode
                        if (bluetoothMode !== BluetoothMode.None)
                            BluetoothManager.onStartBluetooth(bluetoothMode)
                    }
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
     * Do we have a connection to a band leader?
     */
    val isConnectedToServer: Boolean
        get()= mReceiverTask.getReceivers().isNotEmpty()

    /**
     * As a band leader, how many band members are we connected to?
     */
    val bluetoothClientCount: Int
        get() = mSenderTask.getSenders().size

    /**
     * Called when Bluetooth is switched off.
     */
    private fun onStopBluetooth() {
        shutDownBluetoothServer()
        shutDownBluetoothClient()
    }

    /**
     * Shuts down the Bluetooth server, stops the server thread, and disconnects all connected clients.
     */
    private fun shutDownBluetoothServer() {
        synchronized(mBluetoothThreadsLock) {
            mServerBluetoothThread?.run{
                try {
                    stopListening()
                    interrupt()
                    join()
                } catch (e: Exception) {
                    Log.e(BLUETOOTH_TAG, "Error stopping BlueTooth server connection accepting thread, on thread join.", e)
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
        synchronized(mBluetoothThreadsLock) {
            for (thread in mConnectToServerThreads)
                try {
                    with(thread)
                    {
                        stopTrying()
                        interrupt()
                        join()
                    }
                } catch (e: Exception) {
                    Log.e(BLUETOOTH_TAG, "Error stopping BlueTooth client connection thread, on thread join.", e)
                }

            mConnectToServerThreads.clear()
        }
    }

    /**
     * Called when Bluetooth functionality is switched on.
     */
    private fun onStartBluetooth(mode: BluetoothMode) {
        if (mBluetoothAdapter != null)
            startBluetoothWatcherThreads(mBluetoothAdapter!!,mode)
    }

    /**
     * Starts up all the Bluetooth connection-watcher threads.
     */
    private fun startBluetoothWatcherThreads(bluetoothAdapter:BluetoothAdapter,mode: BluetoothMode)
    {
        if (bluetoothAdapter.isEnabled) {
            synchronized(mBluetoothThreadsLock) {
                if (mode === BluetoothMode.Server) {
                    shutDownBluetoothClient()
                    if (mServerBluetoothThread == null) {
                        mServerBluetoothThread = ServerThread(bluetoothAdapter).apply{start()}
                    }
                } else if (mode === BluetoothMode.Client) {
                    shutDownBluetoothServer()
                    if (mConnectToServerThreads.size == 0) {
                        for (device in bluetoothAdapter.bondedDevices)
                            try {
                                mConnectToServerThreads.add(ConnectToServerThread(device))
                            } catch (e: Exception) {
                                Log.e(BLUETOOTH_TAG, "Failed to create ConnectToServerThread for bluetooth device " + device.name, e)
                            }
                        for (thread in mConnectToServerThreads)
                            try {
                                thread.start()
                            } catch (e: Exception) {
                                Log.e(BLUETOOTH_TAG, "Failed to start a ConnectToServerThread", e)
                            }
                    }
                }
            }
        }
    }

    /**
     * Adds a new connection to the pool of connected clients, and informs the user about the
     * new connection.
     */
    internal fun handleConnectionFromClient(socket: BluetoothSocket) {
        if (bluetoothMode === BluetoothMode.Server) {
            EventHandler.sendEventToSongList(EventHandler.CLIENT_CONNECTED, socket.remoteDevice.name)
            mSenderTask.addSender(Sender(socket))
        }
    }

    /**
     * Sets the server connection socket once we connect.
     */
    internal fun setServerConnection(socket: BluetoothSocket) {
        EventHandler.sendEventToSongList(EventHandler.SERVER_CONNECTED, socket.remoteDevice.name)
        mReceiverTask.addReceiver(Receiver(socket))
    }

    /**
     * Called when the app ends. Shuts down all Bluetooth functionality and unregisters
     * us from any Bluetooth events.
     */
    fun shutdown(application: BeatPrompterApplication) {
        if(mBluetoothAdapter!=null) {
            BeatPrompterApplication.preferences.unregisterOnSharedPreferenceChangeListener(this)
            application.unregisterReceiver(mAdapterReceiver)
            shutDownBluetoothClient()
            shutDownBluetoothServer()
        }
    }

    /**
     * Called when the user changes pertinent Bluetooth preferences.
     */
    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        if (key == BeatPrompterApplication.getResourceString(R.string.pref_bluetoothMode_key)) {
            val mode = bluetoothMode
            if (mode === BluetoothMode.None)
                onStopBluetooth()
            else
                onStartBluetooth(mode)
        }
    }
}
