package com.stevenfrew.beatprompter.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.R
import java.io.IOException
import java.util.*

object BluetoothManager:SharedPreferences.OnSharedPreferenceChangeListener {
    val BLUETOOTH_UUID = UUID(0x49ED8190882ADC90L, -0x6c036df6ed2c22d2L)
    const val BLUETOOTH_TAG = "bpbt"

    private var mBluetoothAdapter: BluetoothAdapter? = null

    private val mBluetoothSocketsLock = Any()
    private val mOutputBluetoothSockets = mutableListOf<BluetoothSocket>()
    private var mInputBluetoothSocket: BluetoothSocket? = null

    private val mBluetoothThreadsLock = Any()
    private var mServerBluetoothThread: AcceptConnectionsFromClientsThread? = null
    private val mConnectToServerThreads = mutableListOf<ConnectToServerThread>()

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

    private val mDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action==BluetoothDevice.ACTION_ACL_DISCONNECTED)
            {
                // Something has disconnected.
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                synchronized(mBluetoothSocketsLock) {
                    val matchingSockets=mOutputBluetoothSockets.filter { it.remoteDevice.address==device.address}
                    mOutputBluetoothSockets.removeAll(matchingSockets)
                    matchingSockets.forEach{
                        try {
                            it.close()
                        } catch (e: IOException) {
                            Log.e(BLUETOOTH_TAG, "Error closing socket after client disconnection notification.", e)
                        }
                        EventHandler.sendEventToSongList(EventHandler.CLIENT_DISCONNECTED, device.name)
                    }
                    if (mInputBluetoothSocket != null && mInputBluetoothSocket!!.remoteDevice.address == device.address) {
                        try {
                            mInputBluetoothSocket!!.close()
                        } catch (e: IOException) {
                            Log.e(BLUETOOTH_TAG, "Error closing socket after server disconnection notification.", e)
                        } finally {
                            mInputBluetoothSocket = null
                        }
                        synchronized(mBluetoothThreadsLock) {
                            mConnectToServerThreads.filter { it.isForDevice(device) }.forEach{it.closeSocket()}
                        }
                        EventHandler.sendEventToSongList(EventHandler.SERVER_DISCONNECTED, device.name)
                    }
                }
            }
        }
    }

    // Backwards compatibility with old shite values.
    val bluetoothMode: BluetoothMode
        get() {
            return try {
                BluetoothMode.valueOf(BeatPrompterApplication.preferences.getString(BeatPrompterApplication.getResourceString(R.string.pref_bluetoothMode_key),
                        BeatPrompterApplication.getResourceString(R.string.bluetoothModeNoneValue))!!)
            } catch (e: Exception) {
                BluetoothMode.None
            }

        }

    val isConnectedToServer: Boolean
        get() = synchronized(mBluetoothSocketsLock) {
            return mInputBluetoothSocket != null
        }

    val bluetoothClientCount: Int
        get() = synchronized(mBluetoothSocketsLock) {
            return mOutputBluetoothSockets.size
        }

    fun initialise(application: BeatPrompterApplication) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if(mBluetoothAdapter!=null) {
            application.registerReceiver(mAdapterReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            application.registerReceiver(mDeviceReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
            BeatPrompterApplication.preferences.registerOnSharedPreferenceChangeListener(this)
        }
    }

    private fun onStopBluetooth() {
        shutDownBluetoothServer()
        shutDownBluetoothClient()
    }

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
        synchronized(mBluetoothSocketsLock) {
            mOutputBluetoothSockets.filter{it.isConnected}.forEach{
                try {
                    it.close()
                } catch (e: IOException) {
                    Log.e(BLUETOOTH_TAG, "Error stopping BlueTooth, on socket close.", e)
                }
            }
            mOutputBluetoothSockets.clear()
        }
    }

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
        synchronized(mBluetoothSocketsLock) {
            if (mInputBluetoothSocket != null && mInputBluetoothSocket!!.isConnected)
                try {
                    mInputBluetoothSocket!!.close()
                } catch (ioe: IOException) {
                    Log.e(BLUETOOTH_TAG, "Error stopping BlueTooth, on input socket close.", ioe)
                }
            mInputBluetoothSocket = null
        }
    }

    private fun onStartBluetooth(mode: BluetoothMode) {
        if (mBluetoothAdapter != null)
            startBluetoothWatcherThreads(mBluetoothAdapter!!,mode)
    }

    private fun startBluetoothWatcherThreads(bluetoothAdapter:BluetoothAdapter,mode: BluetoothMode)
    {
        if (bluetoothAdapter.isEnabled) {
            synchronized(mBluetoothThreadsLock) {
                if (mode === com.stevenfrew.beatprompter.bluetooth.BluetoothMode.Server) {
                    shutDownBluetoothClient()
                    if (mServerBluetoothThread == null) {
                        mServerBluetoothThread = AcceptConnectionsFromClientsThread(bluetoothAdapter).apply{start()}
                    }
                } else if (mode === com.stevenfrew.beatprompter.bluetooth.BluetoothMode.Client) {
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

    fun broadcastMessageToClients(message: BluetoothMessage) {
        if (bluetoothMode === BluetoothMode.Server) {
            synchronized(mBluetoothSocketsLock) {
                mOutputBluetoothSockets.filter{it.isConnected}.forEach{
                    try {
                        Log.d(BLUETOOTH_TAG, "Broadcasting message $message to listening apps.")
                        it.outputStream.write(message.bytes)
                    } catch (e: IOException) {
                        Log.e(BLUETOOTH_TAG, "Failed to send Bluetooth message.", e)
                    }
                }
            }
        }
    }

    internal fun handleConnectionFromClient(socket: BluetoothSocket) {
        if (bluetoothMode === BluetoothMode.Server) {
            EventHandler.sendEventToSongList(EventHandler.CLIENT_CONNECTED, socket.remoteDevice.name)
            synchronized(mBluetoothSocketsLock) {
                mOutputBluetoothSockets.add(socket)
            }
        }
    }

    fun startBluetooth() {
        val bluetoothMode = bluetoothMode
        if (bluetoothMode !== BluetoothMode.None)
            onStartBluetooth(bluetoothMode)
    }

    internal fun setServerConnection(socket: BluetoothSocket) {
        EventHandler.sendEventToSongList(EventHandler.SERVER_CONNECTED, socket.remoteDevice.name)
        synchronized(mBluetoothSocketsLock) {
            mInputBluetoothSocket = socket
        }
    }

    fun routeBluetoothMessage(btm: BluetoothMessage) {
        if (btm is ChooseSongMessage)
            EventHandler.sendEventToSongList(EventHandler.BLUETOOTH_MESSAGE_RECEIVED, btm)
        else
            EventHandler.sendEventToSongDisplay(EventHandler.BLUETOOTH_MESSAGE_RECEIVED, btm)
    }

    fun shutdown(application: BeatPrompterApplication) {
        if(mBluetoothAdapter!=null) {
            BeatPrompterApplication.preferences.unregisterOnSharedPreferenceChangeListener(this)
            application.unregisterReceiver(mAdapterReceiver)
            application.unregisterReceiver(mDeviceReceiver)
            shutDownBluetoothClient()
            shutDownBluetoothServer()
        }
    }

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
