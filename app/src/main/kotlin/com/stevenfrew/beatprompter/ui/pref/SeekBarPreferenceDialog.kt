package com.stevenfrew.beatprompter.ui.pref

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.PreferenceDialogFragmentCompat
import com.stevenfrew.beatprompter.R

class SeekBarPreferenceDialog(
	private val suffix: String,
	private val max: Int,
	private val offset: Int
) : PreferenceDialogFragmentCompat(), SeekBar.OnSeekBarChangeListener {
	private var seekBar: SeekBar? = null
	private var valueLabel: TextView? = null

	override fun onBindDialogView(view: View) {
		seekBar = view.findViewById(R.id.seekBarPreferenceSeekBar)
		valueLabel = view.findViewById(R.id.seekBarPreferenceValueLabel)
		seekBar!!.setOnSeekBarChangeListener(this)
		seekBar!!.max = max
		seekBar!!.progress = (this.preference as SeekBarPreference).preferenceValue
		super.onBindDialogView(view)
	}

	@SuppressLint("SetTextI18n")
	override fun onProgressChanged(seek: SeekBar, value: Int, fromTouch: Boolean) {
		val t = "${value + offset}"
		valueLabel!!.text = "$t $suffix"
	}

	override fun onStartTrackingTouch(seek: SeekBar) {}
	override fun onStopTrackingTouch(seek: SeekBar) {}

	override fun onDialogClosed(positiveResult: Boolean) {
		// When the user selects "OK", persist the new value
		if (positiveResult) {
			val value = seekBar!!.progress
			(this.preference as SeekBarPreference).preferenceValue = value
		}
	}

	companion object {
		fun newInstance(
			key: String?,
			suffix: String,
			max: Int,
			offset: Int
		): SeekBarPreferenceDialog {
			val fragment = SeekBarPreferenceDialog(suffix, max, offset)
			val b = Bundle(1)
			b.putString(ARG_KEY, key)
			fragment.arguments = b
			return fragment
		}
	}
}