package com.stevenfrew.beatprompter.comm.midi

import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events

internal class MidiNativeDeviceListener(
	private val mManager: MidiManager,
	private val mSenderTask: SenderTask,
	private val mReceiverTasks: ReceiverTasks,
	private val addDeviceFn: ((deviceInfo: MidiDeviceInfo, manager: MidiManager) -> Unit)?
) : MidiManager.OnDeviceOpenedListener, MidiManager.DeviceCallback() {
	override fun onDeviceAdded(deviceInfo: MidiDeviceInfo) =
		addDeviceFn?.invoke(deviceInfo, mManager) ?: Unit

	override fun onDeviceRemoved(deviceInfo: MidiDeviceInfo) {
		deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)?.also {
			mSenderTask.removeSender(it)
			mReceiverTasks.stopAndRemoveReceiver(it)
		}
	}

	override fun onDeviceOpened(openedDevice: MidiDevice?) {
		try {
			openedDevice?.apply {
				info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)?.also { deviceName ->
					info.ports.forEach {
						when (it.type) {
							MidiDeviceInfo.PortInfo.TYPE_OUTPUT -> mSenderTask.addSender(
								deviceName,
								NativeSender(openedDevice, openedDevice.openInputPort(it.portNumber), deviceName)
							)

							MidiDeviceInfo.PortInfo.TYPE_INPUT -> mReceiverTasks.addReceiver(
								deviceName,
								deviceName,
								NativeReceiver(openedDevice, openedDevice.openOutputPort(it.portNumber), deviceName)
							)
						}
					}
					EventRouter.sendEventToSongList(Events.CONNECTION_ADDED, deviceName)
				}
			}
		} catch (exception: Exception) {
			// Obviously not for us.
		}
	}
}
