package com.stevenfrew.beatprompter.comm.midi

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.media.midi.MidiManager
import android.media.midi.MidiManager.OnDeviceOpenedListener
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.comm.bluetooth.AdapterReceiver
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth
import com.stevenfrew.beatprompter.comm.bluetooth.DeviceReceiver

object BluetoothMidiController {
	// Due to this nonsense:
	// https://developer.android.com/reference/android/content/SharedPreferences.html#registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener)
	// we have to keep a reference to the prefs listener, or it gets garbage collected.
	private var prefsListener: OnSharedPreferenceChangeListener? = null

	fun initialize(
		context: Context,
		senderTask: SenderTask,
		receiverTasks: ReceiverTasks
	) {
		Bluetooth.getBluetoothAdapter(context)?.also { bluetoothAdapter ->
			if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
				val manager =
					context.getSystemService(Context.MIDI_SERVICE) as MidiManager
				val listener = MidiNativeDeviceListener(
					CommunicationType.BluetoothMidi,
					manager,
					senderTask,
					receiverTasks,
					null
				)

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
							override fun onBluetoothDisabled() = onBluetoothStopped(senderTask, receiverTasks)
							override fun onBluetoothEnabled(context: Context) =
								attemptBluetoothMidiConnections(bluetoothAdapter, manager, listener)
						},
						IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
					)
					registerReceiver(
						DeviceReceiver(senderTask, receiverTasks),
						IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
					)
				}
				val prefsListener = OnSharedPreferenceChangeListener { _, key ->
					val midiBluetoothKey =
						BeatPrompter.appResources.getString(R.string.pref_midiConnectionTypes_key)
					val bluetoothMidiDevicesKey =
						BeatPrompter.appResources.getString(R.string.pref_bluetoothMidiDevices_key)
					when (key) {
						bluetoothMidiDevicesKey, midiBluetoothKey -> {
							Logger.logComms("Bluetooth MIDI connection types or target devices changed ... restarting connections.")
							Logger.logComms("Removing all Bluetooth MIDI senders & receivers.")
							onBluetoothStopped(senderTask, receiverTasks)
							if (BeatPrompter.preferences.midiConnectionTypes.contains(ConnectionType.Bluetooth)) {
								Logger.logComms("Bluetooth is still a selected MIDI connection type ... attempting Bluetooth MIDI connections.")
								attemptBluetoothMidiConnections(bluetoothAdapter, manager, listener)
							}
						}
					}
				}
				BeatPrompter.preferences.registerOnSharedPreferenceChangeListener(prefsListener)
				this.prefsListener = prefsListener
				attemptBluetoothMidiConnections(bluetoothAdapter, manager, listener)
			}
		}
	}

	/**
	 * Called when Bluetooth is switched off.
	 */
	private fun onBluetoothStopped(senderTask: SenderTask, receiverTasks: ReceiverTasks) {
		Logger.logComms("Bluetooth has stopped.")
		senderTask.removeAll(CommunicationType.BluetoothMidi)
		receiverTasks.stopAndRemoveAll(CommunicationType.BluetoothMidi)
	}

	private fun attemptBluetoothMidiConnections(
		bluetoothAdapter: BluetoothAdapter,
		manager: MidiManager,
		listener: OnDeviceOpenedListener
	) =
		BeatPrompter.preferences.bluetoothMidiDevices.run {
			Bluetooth.getPairedDevices(bluetoothAdapter).filter { contains(it.address) }
				.forEach { attemptMidiConnection(manager, it, listener) }
		}

	private fun attemptMidiConnection(
		manager: MidiManager,
		device: BluetoothDevice,
		listener: OnDeviceOpenedListener
	) =
		try {
			manager.openBluetoothDevice(
				device,
				listener,
				null
			)
		} catch (_: Exception) {
			// Not for us ...
		}
}
