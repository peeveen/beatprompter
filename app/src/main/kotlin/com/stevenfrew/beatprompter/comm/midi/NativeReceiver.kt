package com.stevenfrew.beatprompter.comm.midi

import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Build
import android.support.annotation.RequiresApi
import com.stevenfrew.beatprompter.comm.ReceiverTask

@RequiresApi(Build.VERSION_CODES.M)
class NativeReceiver(private val mPort: MidiOutputPort,
                     name: String)
    : Receiver(name) {
    private val mInnerReceiver = NativeReceiverReceiver()
    private val mInnerBufferLock = Any()
    private var mInnerBuffer = ByteArray(INITIAL_INNER_BUFFER_SIZE)
    private var mInnerBufferPosition = 0

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mPort.connect(mInnerReceiver)
    }

    override fun close() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mPort.disconnect(mInnerReceiver)
        mPort.close()
    }

    override fun receiveMessageData(buffer: ByteArray, offset: Int, maximumAmount: Int): Int {
        synchronized(mInnerBufferLock)
        {
            return Math.min(maximumAmount, mInnerBufferPosition).also {
                if (it != 0) {
                    System.arraycopy(mInnerBuffer, 0, buffer, offset, it)
                    mInnerBufferPosition -= it
                }
            }
        }
    }

    override fun unregister(task: ReceiverTask) {
        MIDIController.removeReceiver(task)
    }

    inner class NativeReceiverReceiver : MidiReceiver() {
        override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
            if (msg != null) {
                synchronized(mInnerBufferLock)
                {
                    // If we exceed the available space, we have to increase space.
                    // There is no second-chance to get this data.
                    if (mInnerBufferPosition + count > mInnerBuffer.size) {
                        val biggerBufferSize = Math.max(mInnerBuffer.size + INNER_BUFFER_GROW_SIZE, mInnerBufferPosition + count)
                        val biggerBuffer = ByteArray(biggerBufferSize)
                        System.arraycopy(mInnerBuffer, 0, biggerBuffer, 0, mInnerBuffer.size)
                        mInnerBuffer = biggerBuffer
                    }
                    System.arraycopy(msg, offset, mInnerBuffer, mInnerBufferPosition, count)
                    mInnerBufferPosition += count
                }
            }
        }
    }

    companion object {
        private const val INITIAL_INNER_BUFFER_SIZE = 4096
        private const val INNER_BUFFER_GROW_SIZE = 2048
    }
}