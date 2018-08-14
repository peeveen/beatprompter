package com.stevenfrew.beatprompter.midi

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import kotlin.experimental.and

class USBInTask constructor(connection: UsbDeviceConnection, endpoint: UsbEndpoint, incomingChannels: Int):USBTask(connection,endpoint,true) {

    private val mBufferSize=endpoint.maxPacketSize
    private val mBuffer=ByteArray(mBufferSize)
    private var mIncomingChannels=incomingChannels
    private val mIncomingChannelsLock = Any()

    init
    {
        // Read all incoming data until there is none left. Basically, clear anything that
        // has been buffered before we accepted the connection.
        // On the offchance that there is a neverending barrage of data coming in at a ridiculous
        // speed, let's say 1K of data, max, to prevent lockup.
        var bufferClear = 0
        var dataRead: Int
        do {
            dataRead = connection.bulkTransfer(endpoint, mBuffer, mBufferSize, 250)
            if (dataRead > 0)
                bufferClear += dataRead
        } while (dataRead > 0 && dataRead == mBufferSize && !shouldStop && bufferClear < 1024)
    }

    override fun doWork() {
        var inSysEx = false
        var preBuffer: ByteArray? = null
        val endpoint = endpoint
        val connection = connection

        while (!shouldStop) {
            var dataRead = connection!!.bulkTransfer(endpoint, mBuffer, mBufferSize, 1000)
            if(dataRead>0) {

                val workBuffer: ByteArray
                if (preBuffer == null)
                    workBuffer = mBuffer
                else {
                    workBuffer = ByteArray(preBuffer.size + dataRead)
                    System.arraycopy(preBuffer, 0, workBuffer, 0, preBuffer.size)
                    System.arraycopy(mBuffer, 0, workBuffer, preBuffer.size, dataRead)
                    dataRead += preBuffer.size
                    preBuffer = null
                }
                var f = 0
                while (f < dataRead) {
                    val messageByte = workBuffer[f]
                    // All interesting MIDI signals have the top bit set.
                    if (messageByte and 0x80.toByte() != 0.toByte()) {
                        if (inSysEx) {
                            if (messageByte == Message.MIDI_SYSEX_END_BYTE) {
                                Log.d(MIDIController.MIDI_TAG, "Received MIDI SysEx end message.")
                                inSysEx = false
                            }
                        } else {
                            if (messageByte == Message.MIDI_START_BYTE || messageByte == Message.MIDI_CONTINUE_BYTE || messageByte == Message.MIDI_STOP_BYTE) {
                                // These are single byte messages.
                                try {
                                    MIDIController.mMIDISongDisplayInQueue.put(IncomingMessage(byteArrayOf(messageByte)))
                                    if (messageByte == Message.MIDI_START_BYTE)
                                        Log.d(MIDIController.MIDI_TAG, "Received MIDI Start message.")
                                    else if (messageByte == Message.MIDI_CONTINUE_BYTE)
                                        Log.d(MIDIController.MIDI_TAG, "Received MIDI Continue message.")
                                    if (messageByte == Message.MIDI_STOP_BYTE)
                                        Log.d(MIDIController.MIDI_TAG, "Received MIDI Stop message.")
                                } catch (ie: InterruptedException) {
                                    Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to write new MIDI message to queue.", ie)
                                }

                            } else if (messageByte == Message.MIDI_SONG_POSITION_POINTER_BYTE) {
                                // This message requires two additional bytes.
                                when {
                                    f < dataRead - 2 -> try {
                                        val msg = IncomingSongPositionPointerMessage(byteArrayOf(messageByte, workBuffer[++f], workBuffer[++f]))
                                        Log.d(MIDIController.MIDI_TAG, "Received MIDI Song Position Pointer message: " + msg.toString())
                                        MIDIController.mMIDISongDisplayInQueue.put(msg)
                                    } catch (ie: InterruptedException) {
                                        Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to write new MIDI message to queue.", ie)
                                    }
                                    f < dataRead - 1 -> preBuffer = byteArrayOf(messageByte, workBuffer[++f])
                                    else -> preBuffer = byteArrayOf(messageByte)
                                }
                            } else if (messageByte == Message.MIDI_SONG_SELECT_BYTE) {
                                if (f < dataRead - 1)
                                    try {
                                        val msg = IncomingMessage(byteArrayOf(messageByte, workBuffer[++f]))
                                        Log.d(MIDIController.MIDI_TAG, "Received MIDI SongSelect message: " + msg.toString())
                                        MIDIController.mMIDISongListInQueue.put(msg)
                                    } catch (ie: InterruptedException) {
                                        Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to write new MIDI message to queue.", ie)
                                    }
                                else
                                    preBuffer = byteArrayOf(workBuffer[f])
                            } else if (messageByte == Message.MIDI_SYSEX_START_BYTE) {
                                Log.d(MIDIController.MIDI_TAG, "Received MIDI SysEx start message.")
                                inSysEx = true
                            } else {
                                val channelsToListenTo = getIncomingChannels()
                                val messageByteWithoutChannel = (messageByte and 0xF0.toByte())
                                // System messages start with 0xF0, we're past caring about those.
                                if (messageByteWithoutChannel != 0xF0.toByte()) {
                                    val channel = (messageByte and 0x0F)
                                    if (channelsToListenTo and (1 shl channel.toInt()) != 0) {
                                        if (messageByteWithoutChannel == Message.MIDI_PROGRAM_CHANGE_BYTE) {
                                            if (f < dataRead - 1)
                                                try {
                                                    val msg = IncomingMessage(byteArrayOf(messageByte, workBuffer[++f]))
                                                    Log.d(MIDIController.MIDI_TAG, "Received MIDI ProgramChange message: " + msg.toString())
                                                    MIDIController.mMIDISongListInQueue.put(msg)
                                                } catch (ie: InterruptedException) {
                                                    Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to write new MIDI message to queue.", ie)
                                                }
                                            else
                                                preBuffer = byteArrayOf(messageByte)
                                        } else if (messageByteWithoutChannel == Message.MIDI_CONTROL_CHANGE_BYTE) {
                                            // This message requires two additional bytes.
                                            when {
                                                f < dataRead - 2 -> try {
                                                    val msg = IncomingMessage(byteArrayOf(messageByte, workBuffer[++f], workBuffer[++f]))
                                                    if (msg.isLSBBankSelect() || msg.isMSBBankSelect()) {
                                                        Log.d(MIDIController.MIDI_TAG, "Received MIDI Control Change message: " + msg.toString())
                                                        MIDIController.mMIDISongListInQueue.put(msg)
                                                    }
                                                } catch (ie: InterruptedException) {
                                                    Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to write new MIDI message to queue.", ie)
                                                }
                                                f < dataRead - 1 -> preBuffer = byteArrayOf(messageByte, workBuffer[++f])
                                                else -> preBuffer = byteArrayOf(messageByte)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ++f
                }
            }
        }
    }


    private fun getIncomingChannels(): Int {
        synchronized(mIncomingChannelsLock) {
            return mIncomingChannels
        }
    }

    fun setIncomingChannels(channels: Int) {
        synchronized(mIncomingChannelsLock) {
            mIncomingChannels = channels
        }
    }
}