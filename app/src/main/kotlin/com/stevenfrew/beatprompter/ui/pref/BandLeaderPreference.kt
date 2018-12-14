package com.stevenfrew.beatprompter.ui.pref

import android.app.AlertDialog
import android.content.Context
import android.preference.ListPreference
import android.util.AttributeSet
import android.widget.ArrayAdapter
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothController

class BandLeaderPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {
    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        BluetoothController.getPairedDevices().apply {
            entries = map { it.name }.toTypedArray()
            entryValues = map { it.address }.toTypedArray()
        }
        val listAdapter = ArrayAdapter(context, android.R.layout.select_dialog_singlechoice, entries)
        builder.setAdapter(listAdapter, this)
        super.onPrepareDialogBuilder(builder)
    }
}