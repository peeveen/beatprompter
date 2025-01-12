package com.stevenfrew.beatprompter.comm.midi

import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.ConnectionDescriptor
import com.stevenfrew.beatprompter.comm.ConnectionNotificationTask
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask

internal class MidiNativeDeviceListener(
	private val commType: CommunicationType,
	private val manager: MidiManager,
	private val senderTask: SenderTask,
	private val receiverTasks: ReceiverTasks,
	private val addDeviceFn: ((deviceInfo: MidiDeviceInfo, manager: MidiManager) -> Unit)?
) : MidiManager.OnDeviceOpenedListener, MidiManager.DeviceCallback() {
	override fun onDeviceAdded(deviceInfo: MidiDeviceInfo) =
		addDeviceFn?.invoke(deviceInfo, manager) ?: Unit

	override fun onDeviceRemoved(deviceInfo: MidiDeviceInfo) {
		deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)?.also {
			senderTask.removeSender(it)
			receiverTasks.stopAndRemoveReceiver(it)
		}
	}

	override fun onDeviceOpened(openedDevice: MidiDevice?) {
		try {
			openedDevice?.apply {
				info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)?.also { deviceName ->
					info.ports.forEach {
						when (it.type) {
							MidiDeviceInfo.PortInfo.TYPE_OUTPUT -> senderTask.addSender(
								deviceName,
								NativeSender(
									openedDevice,
									openedDevice.openInputPort(it.portNumber),
									deviceName,
									commType
								)
							)

							MidiDeviceInfo.PortInfo.TYPE_INPUT -> receiverTasks.addReceiver(
								deviceName,
								deviceName,
								NativeReceiver(
									openedDevice,
									openedDevice.openOutputPort(it.portNumber),
									deviceName,
									commType
								)
							)
						}
					}
					ConnectionNotificationTask.addConnection(
						ConnectionDescriptor(
							deviceName,
							commType
						)
					)
				}
			}
		} catch (_: Exception) {
			// Obviously not for us.
		}
	}
}
