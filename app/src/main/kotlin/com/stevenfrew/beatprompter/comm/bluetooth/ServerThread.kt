package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.BeatPrompterLogger
import java.io.IOException
import java.util.*

/**
 * This class is a thread that runs when the app is in BlueToothServer mode. It listens for connections from
 * paired clients, and creates an output socket from each connection. Any events broadcast from this instance
 * of the app will be sent to all output sockets.
 */
class ServerThread internal constructor(private val mBluetoothAdapter: BluetoothAdapter,
                                        private val mUUID: UUID,
                                        private val mOnConnectedFunction: (socket: BluetoothSocket) -> Unit)
    : Thread() {
    private var mmServerSocket: BluetoothServerSocket? = null
    private var mStop = false
    private val mSocketNullLock = Any()

    override fun run() {
        // Keep listening until exception occurs or a socket is returned
        while (!mStop) {
            try {
                var serverSocket: BluetoothServerSocket?
                synchronized(mSocketNullLock) {
                    if (mmServerSocket == null) {
                        try {
                            // MY_UUID is the app's UUID string, also used by the server code
                            mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(BeatPrompterApplication.APP_NAME, mUUID)
                            BeatPrompterLogger.logComms("Created the Bluetooth server socket.")
                        } catch (e: IOException) {
                            BeatPrompterLogger.logComms("Error creating Bluetooth server socket.", e)
                        }
                    }
                    serverSocket = mmServerSocket
                }

                // If a connection was accepted
                // Do work to manage the connection (in a separate thread)
                BeatPrompterLogger.logComms("Looking for a client connection.")
                serverSocket?.accept(5000)?.also {
                    BeatPrompterLogger.logComms("Found a client connection.")
                    mOnConnectedFunction(it)
                }
            } catch (e: Exception) {
                //Log.e(BLUETOOTH_TAG, "Failed to accept new Bluetooth connection.",e);
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish  */
    fun stopListening() {
        mStop = true
        synchronized(mSocketNullLock) {
            try {
                BeatPrompterLogger.logComms("Closing Bluetooth server socket.")
                mmServerSocket?.close()
                BeatPrompterLogger.logComms("Closed Bluetooth server socket.")
            } catch (e: IOException) {
                BeatPrompterLogger.logComms("Failed to close Bluetooth listener socket.", e)
            } finally {
                mmServerSocket = null
            }
        }
    }
}
