package com.stevenfrew.beatprompter.ui.pref

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth

class BluetoothMidiDevicesPreference(context: Context, attrs: AttributeSet) :
	MultiSelectListPreference(context, attrs) {
	private val bluetoothDevices: List<BluetoothDevice> = Bluetooth.getPairedDevices(context)

	override fun getEntries(): Array<CharSequence> = bluetoothDevices.mapNotNull {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
				it.alias ?: it.name
			else
				it.name
		} catch (_: SecurityException) {
			null
		}
	}.toTypedArray()

	override fun getEntryValues(): Array<CharSequence> =
		bluetoothDevices.map { it.address }.toTypedArray()
}