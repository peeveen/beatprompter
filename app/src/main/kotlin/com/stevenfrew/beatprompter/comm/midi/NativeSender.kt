package com.stevenfrew.beatprompter.comm.midi

import android.media.midi.MidiInputPort
import com.stevenfrew.beatprompter.comm.SenderBase
import com.stevenfrew.beatprompter.comm.midi.message.outgoing.OutgoingMessage

class NativeSender(private val mPort: MidiInputPort): SenderBase<OutgoingMessage>() {
    override fun sendMessageData(bytes: ByteArray, length: Int) {
        mPort.send(bytes,0,length)
    }

    override fun close() {
        mPort.close()
    }
}