package com.stevenfrew.beatprompter.comm.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events

class NativeMidiController(
	context: Context,
	internal val mSenderTask: SenderTask,
	internal val mReceiverTasks: ReceiverTasks
) {
	private lateinit var mManager: MidiManager
	private var mDeviceListener: MidiNativeDeviceListener? = null

	init {
		if (context.packageManager.hasSystemFeature(Context.MIDI_SERVICE)) {
			mDeviceListener = MidiNativeDeviceListener()
			mManager =
				context.getSystemService(Context.MIDI_SERVICE) as MidiManager
			mManager.apply {
				registerDeviceCallback(mDeviceListener, null)
				devices?.forEach {
					addNativeDevice(it)
				}
			}
		}
	}

	private fun addNativeDevice(nativeDeviceInfo: MidiDeviceInfo) {
		if (Preferences.midiConnectionType == ConnectionType.Native)
			mManager.openDevice(nativeDeviceInfo, mDeviceListener, null)
	}

	inner class MidiNativeDeviceListener : MidiManager.OnDeviceOpenedListener,
		MidiManager.DeviceCallback() {
		override fun onDeviceAdded(deviceInfo: MidiDeviceInfo) {
			addNativeDevice(deviceInfo)
		}

		override fun onDeviceRemoved(deviceInfo: MidiDeviceInfo) {
			deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)?.also {
				this@NativeMidiController.mSenderTask.removeSender(it)
				this@NativeMidiController.mReceiverTasks.stopAndRemoveReceiver(it)
			}
		}

		override fun onDeviceOpened(openedDevice: MidiDevice?) {
			try {
				openedDevice?.apply {
					info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)?.also { deviceName ->
						info.ports.forEach {
							when (it.type) {
								MidiDeviceInfo.PortInfo.TYPE_INPUT -> this@NativeMidiController.mSenderTask.addSender(
									deviceName,
									NativeSender(openedDevice.openInputPort(it.portNumber), deviceName)
								)

								MidiDeviceInfo.PortInfo.TYPE_OUTPUT -> this@NativeMidiController.mReceiverTasks.addReceiver(
									deviceName,
									deviceName,
									NativeReceiver(openedDevice.openOutputPort(it.portNumber), deviceName)
								)
							}
						}
						EventRouter.sendEventToSongList(Events.CONNECTION_ADDED, deviceName)
					}
				}
			} catch (ioException: Exception) {
				// Obviously not for us.
			}
		}
	}
}