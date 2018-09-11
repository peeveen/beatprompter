package com.stevenfrew.beatprompter.comm.midi

import android.media.midi.MidiInputPort
import android.os.Build
import android.support.annotation.RequiresApi
import com.stevenfrew.beatprompter.comm.SenderBase

@RequiresApi(Build.VERSION_CODES.M)
class NativeSender(private val mPort: MidiInputPort): SenderBase() {
    override fun sendMessageData(bytes: ByteArray, length: Int) {
        mPort.send(bytes,0,length)
    }

    override fun close() {
        mPort.close()
    }
}