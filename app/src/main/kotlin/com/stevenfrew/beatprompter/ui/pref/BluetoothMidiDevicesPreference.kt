package com.stevenfrew.beatprompter.ui.pref

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth

class BluetoothMidiDevicesPreference(context: Context, attrs: AttributeSet) :
	MultiSelectListPreference(context, attrs) {
	private val bluetoothDevices: List<BluetoothDevice> = Bluetooth.getPairedDevices(context)

	override fun getEntries(): Array<CharSequence> = bluetoothDevices.map { it.name }.toTypedArray()

	override fun getEntryValues(): Array<CharSequence> =
		bluetoothDevices.map { it.address }.toTypedArray()
}