package com.stevenfrew.beatprompter.comm.midi

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask

object NativeMidiController {
	private var deviceListener: MidiNativeDeviceListener? = null

	fun initialize(
		context: Context,
		senderTask: SenderTask,
		receiverTasks: ReceiverTasks
	) {
		if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
			val manager =
				context.getSystemService(Context.MIDI_SERVICE) as MidiManager
			deviceListener =
				MidiNativeDeviceListener(
					CommunicationType.Midi,
					manager,
					senderTask,
					receiverTasks
				) { deviceInfo, midiManager -> addNativeDevice(deviceInfo, midiManager) }
			manager.apply {
				registerDeviceCallback(deviceListener, null)
				devices?.forEach {
					addNativeDevice(it, manager)
				}
			}
		}
	}

	private fun addNativeDevice(nativeDeviceInfo: MidiDeviceInfo, manager: MidiManager) {
		if (Preferences.midiConnectionTypes.contains(ConnectionType.Native))
			manager.openDevice(nativeDeviceInfo, deviceListener, null)
	}
}