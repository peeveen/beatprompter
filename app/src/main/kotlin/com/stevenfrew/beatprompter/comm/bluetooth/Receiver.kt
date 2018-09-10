package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.stevenfrew.beatprompter.comm.ReceiverBase
import com.stevenfrew.beatprompter.comm.bluetooth.message.IncomingMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.NotEnoughDataException
import com.stevenfrew.beatprompter.comm.bluetooth.message.UnknownMessageException

class Receiver(private val mmSocket: BluetoothSocket): ReceiverBase() {
    override fun receiveMessageData(buffer: ByteArray, offset: Int, maximumAmount: Int): Int {
        return mmSocket.inputStream.read(buffer, offset, maximumAmount)
    }

    override fun parseMessageData(buffer: ByteArray, dataStart: Int, dataEnd: Int): Int {
        var bufferCopy=buffer
        var bufferStart=dataStart
        while (dataStart < dataEnd) {
            try {
                val btm = IncomingMessage.fromBytes(bufferCopy)
                val messageLength = btm.messageLength
                bufferStart += messageLength
                bufferCopy=buffer.copyOfRange(bufferStart,dataEnd)
                BluetoothManager.routeBluetoothMessage(btm.receivedMessage)
            } catch (exception: NotEnoughDataException) {
                // Read again!
                Log.d(BluetoothManager.BLUETOOTH_TAG, "Not enough data in the Bluetooth buffer to create a fully formed message, waiting for more data.")
                return 0
            } catch (exception: UnknownMessageException) {
                Log.d(BluetoothManager.BLUETOOTH_TAG, "Unknown Bluetooth message received.")
                return 1
            }
        }
        return bufferStart-dataStart
    }

    override fun close() {
        mmSocket.close()
    }
}