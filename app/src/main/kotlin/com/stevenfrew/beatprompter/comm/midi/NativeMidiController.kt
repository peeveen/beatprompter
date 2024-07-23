package com.stevenfrew.beatprompter.comm.midi

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask

class NativeMidiController(
	context: Context,
	senderTask: SenderTask,
	receiverTasks: ReceiverTasks
) {
	private var mDeviceListener: MidiNativeDeviceListener? = null

	init {
		if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
			val manager =
				context.getSystemService(Context.MIDI_SERVICE) as MidiManager
			mDeviceListener =
				MidiNativeDeviceListener(
					manager,
					senderTask,
					receiverTasks
				) { deviceInfo, midiManager -> addNativeDevice(deviceInfo, midiManager) }
			manager.apply {
				registerDeviceCallback(mDeviceListener, null)
				devices?.forEach {
					addNativeDevice(it, manager)
				}
			}
		}
	}

	private fun addNativeDevice(nativeDeviceInfo: MidiDeviceInfo, manager: MidiManager) {
		if (Preferences.midiConnectionTypes.contains(ConnectionType.Native))
			manager.openDevice(nativeDeviceInfo, mDeviceListener, null)
	}
}