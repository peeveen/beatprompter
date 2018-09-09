package com.stevenfrew.beatprompter.comm.midi.message.incoming

import com.stevenfrew.beatprompter.comm.midi.message.Message

class ControlChangeMessage(private val mController:Byte,val mValue:Byte,val mChannel:Byte):IncomingMessage()
{
    internal fun isMSBBankSelect(): Boolean {
        return mController == Message.MIDI_MSB_BANK_SELECT_CONTROLLER
    }

    internal fun isLSBBankSelect(): Boolean {
        return mController == Message.MIDI_LSB_BANK_SELECT_CONTROLLER
    }
}