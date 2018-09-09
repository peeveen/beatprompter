package com.stevenfrew.beatprompter.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException

/**
 * A thread that continuously attempts to connect to a band leader.
 */
internal class ConnectToServerThread(private val mDevice: BluetoothDevice) : Thread() {
    private var mmSocket: BluetoothSocket? = null
    private var mStop = false
    private val mSocketNullLock = Any()

    fun isForDevice(device: BluetoothDevice): Boolean {
        return mDevice.address == device.address
    }

    override fun run() {
        while (!mStop) {
            if (!BluetoothManager.isConnectedToServer)
                try {
                    // Connect the device through the socket. This will block
                    // until it succeeds or throws an exception, which can happen
                    // if it doesn't find anything to connect to within about 4 seconds.
                    var socket: BluetoothSocket?=null
                    synchronized(mSocketNullLock) {
                        if (mmSocket == null) {
                            try {
                                // MY_UUID is the app's UUID string, also used by the server code
                                mmSocket = mDevice.createRfcommSocketToServiceRecord(BluetoothManager.BLUETOOTH_UUID)
                            } catch (e: IOException) {
                                Log.e(BluetoothManager.BLUETOOTH_TAG, "Error creating Bluetooth socket.", e)
                            }

                        }
                        socket = mmSocket
                    }
                    if (socket != null)
                        startConnectionThread(socket!!)
                } catch (connectException: IOException) {
                    Log.e(BluetoothManager.BLUETOOTH_TAG, "Failed to connect to a BeatPrompter band leader (there probably isn't one!)", connectException)
                }
            else
                // Already connected. Wait a bit and try/check again.
                try {
                    Thread.sleep(5000)
                } catch (ie: InterruptedException) {
                    Log.w(BluetoothManager.BLUETOOTH_TAG, "Thread that maintains connection to the server was interrupted while waiting.")
                }
        }
    }

    /**
     * Got a connection, so add it to the pile of maintained connections.
     */
    private fun startConnectionThread(socket:BluetoothSocket)
    {
        socket.connect()
        // Do work to manage the connection (in a separate thread)
        BluetoothManager.setServerConnection(socket)
        val connectedClientThread = ConnectedClientThread(socket)
        connectedClientThread.start()
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
    internal fun closeSocket() {
        synchronized(mSocketNullLock) {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(BluetoothManager.BLUETOOTH_TAG, "Error closing Bluetooth socket.", e)
            } finally {
                mmSocket = null
            }
        }
    }
}