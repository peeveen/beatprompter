package com.stevenfrew.beatprompter.comm.midi

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.midi.MidiManager
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.comm.bluetooth.AdapterReceiver
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth
import com.stevenfrew.beatprompter.comm.bluetooth.DeviceReceiver

class BluetoothMidiController(
	context: Context,
	senderTask: SenderTask,
	receiverTasks: ReceiverTasks
) {
	private var mBluetoothListener: MidiNativeDeviceListener? = null

	// Threads that watch for client/server connections, and an object to synchronize their
	// use.
	private var mBluetoothAdapter: BluetoothAdapter? = null

	init {
		Bluetooth.getBluetoothAdapter(context)?.also {
			if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
				val manager =
					context.getSystemService(Context.MIDI_SERVICE) as MidiManager
				mBluetoothListener = MidiNativeDeviceListener(manager, senderTask, receiverTasks, null)

				mBluetoothAdapter = it
				Logger.logComms("Bluetooth adapter found.")
				Logger.logComms("Starting BandBluetooth sender thread.")
				Logger.logComms("BandBluetooth sender thread started.")

				context.apply {
					registerReceiver(
						/**
						 * User could switch Bluetooth functionality on/off at any time.
						 * We need to keep an eye on that.
						 */
						object : AdapterReceiver() {
							override fun onBluetoothDisabled() = onStopBluetooth()
							override fun onBluetoothEnabled(context: Context) =
								attemptMidiConnections(context, manager)
						},
						IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
					)
					registerReceiver(
						DeviceReceiver(senderTask, receiverTasks),
						IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
					)
				}
				Preferences.registerOnSharedPreferenceChangeListener { _, key ->
					val midiBluetoothKey =
						BeatPrompter.appResources.getString(R.string.pref_midiConnectionTypes_key)
					when (key) {
						midiBluetoothKey -> {
							Logger.logComms("Bluetooth MIDI connection types changed.")
							if (Preferences.midiConnectionTypes.contains(ConnectionType.Bluetooth))
								attemptMidiConnections(context, manager)
							else
								onStopBluetooth()
						}
					}
				}
				attemptMidiConnections(context, manager)
			}
		}
	}

	/**
	 * Called when Bluetooth is switched off.
	 */
	private fun onStopBluetooth() {
		Logger.logComms("Bluetooth has stopped.")
	}

	private fun attemptMidiConnections(context: Context, manager: MidiManager) =
		Bluetooth.getPairedDevices(context).forEach { attemptMidiConnection(manager, it) }

	private fun attemptMidiConnection(
		manager: MidiManager,
		device: BluetoothDevice
	) =
		try {
			manager.openBluetoothDevice(
				device,
				mBluetoothListener,
				null
			)
		} catch (e: Exception) {
			// Not for us ...
		}
}
