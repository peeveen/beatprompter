package com.stevenfrew.beatprompter.ui.pref

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.PreferenceDialogFragmentCompat
import com.stevenfrew.beatprompter.R

class SeekBarPreferenceDialog(
	private val mSuffix: String,
	private val mMax: Int,
	private val mOffset: Int
) : PreferenceDialogFragmentCompat(), SeekBar.OnSeekBarChangeListener {
	private var mSeekBar: SeekBar? = null
	private var mValueLabel: TextView? = null

	override fun onBindDialogView(view: View) {
		mSeekBar = view.findViewById(R.id.seekBarPreferenceSeekBar)
		mValueLabel = view.findViewById(R.id.seekBarPreferenceValueLabel)
		mSeekBar!!.setOnSeekBarChangeListener(this)
		mSeekBar!!.max = mMax
		mSeekBar!!.progress = (this.preference as SeekBarPreference).getPreferenceValue()
		super.onBindDialogView(view)
	}

	override fun onProgressChanged(seek: SeekBar, value: Int, fromTouch: Boolean) {
		val t = "${value + mOffset}"
		mValueLabel!!.text = "$t $mSuffix"
	}

	override fun onStartTrackingTouch(seek: SeekBar) {}
	override fun onStopTrackingTouch(seek: SeekBar) {}

	override fun onDialogClosed(positiveResult: Boolean) {
		// When the user selects "OK", persist the new value
		if (positiveResult) {
			val value = mSeekBar!!.progress
			(this.preference as SeekBarPreference).setPreferenceValue(value)
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