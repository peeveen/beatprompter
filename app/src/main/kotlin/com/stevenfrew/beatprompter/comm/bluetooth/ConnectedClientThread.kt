package com.stevenfrew.beatprompter.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.stevenfrew.beatprompter.bluetooth.message.BluetoothMessage
import com.stevenfrew.beatprompter.bluetooth.message.NotEnoughBluetoothDataException
import com.stevenfrew.beatprompter.bluetooth.message.UnknownBluetoothMessageException
import java.io.IOException
import java.io.InputStream

/**
 * Thread for band members. Watches the provided Bluetooth socket for incoming commands from the
 * band leader.
 */
internal class ConnectedClientThread constructor(private val mmSocket: BluetoothSocket) : Thread() {
    // "The input stream will be returned even if the socket is not yet connected, but operations on
    // that stream will throw IOException until the associated socket is connected."
    private val mmInStream: InputStream=mmSocket.inputStream

    override fun run() {
        val buffer = ByteArray(2048)  // buffer store for the stream
        var bytes: Int // bytes returned from read()
        var bufferContentsLength = 0

        // Keep listening to the InputStream until an exception occurs
        while (mmSocket.isConnected) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer, bufferContentsLength, 2048 - bufferContentsLength)
                // Send the obtained bytes to the UI activity
                if (bytes > 0) {
                    bufferContentsLength += bytes
                    while (bufferContentsLength > 0) {
                        try {
                            val btm = BluetoothMessage.fromBytes(buffer)
                            val messageLength = btm.messageLength
                            bufferContentsLength -= messageLength
                            System.arraycopy(buffer, messageLength, buffer, 0, bufferContentsLength)
                            BluetoothManager.routeBluetoothMessage(btm.receivedMessage)
                        } catch (exception: NotEnoughBluetoothDataException) {
                            // Read again!
                            Log.d(BluetoothManager.BLUETOOTH_TAG, "Not enough data in the Bluetooth buffer to create a fully formed message, waiting for more data.")
                            break
                        } catch (exception: UnknownBluetoothMessageException) {
                            Log.d(BluetoothManager.BLUETOOTH_TAG, "Unknown Bluetooth message received.")
                            bufferContentsLength=0 // Get rid of the bad data (possibly more, but by this stage, we're probably lost)
                            break
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(BluetoothManager.BLUETOOTH_TAG, "Failed to read or route the received Bluetooth message.", e)
            }
        }
    }
}