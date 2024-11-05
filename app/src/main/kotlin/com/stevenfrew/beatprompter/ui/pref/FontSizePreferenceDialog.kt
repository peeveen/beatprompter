package com.stevenfrew.beatprompter.ui.pref

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.PreferenceDialogFragmentCompat
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import java.util.Locale


class FontSizePreferenceDialog : PreferenceDialogFragmentCompat(), SeekBar.OnSeekBarChangeListener {
	private var seekBar: SeekBar? = null
	private var textView: TextView? = null
	private var currentValue = -1

	override fun onBindDialogView(view: View) {
		seekBar = view.findViewById(R.id.fontSizeSeekBar)
		textView = view.findViewById(R.id.fontSizeTextView)
		seekBar!!.setOnSeekBarChangeListener(this)
		currentValue = (preference as FontSizePreference).fontSize
		seekBar!!.progress = currentValue
		textView!!.text = String.format(Locale.getDefault(), "%d", currentValue)
		//mSeekBar!!.min= FontSizePreference.FONT_SIZE_MIN
		seekBar!!.max = FontSizePreference.FONT_SIZE_MAX
		super.onBindDialogView(view)
	}

	override fun onDialogClosed(positiveResult: Boolean) {
		// When the user selects "OK", persist the new value
		if (positiveResult) {
			val value = seekBar!!.progress
			(preference as FontSizePreference).fontSize = value
		}
	}

	override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
		currentValue = progress
		val size = currentValue + FontSizePreference.FONT_SIZE_OFFSET
		textView!!.text = String.format(Locale.getDefault(), "%d", size)
		textView!!.setTextSize(
			TypedValue.COMPLEX_UNIT_PX,
			size * BeatPrompter.platformUtils.fontManager.fontScaling
		)
	}

	override fun onStartTrackingTouch(seekBar: SeekBar) {}
	override fun onStopTrackingTouch(seekBar: SeekBar) {}

	companion object {
		fun newInstance(
			key: String?
		): FontSizePreferenceDialog {
			val fragment = FontSizePreferenceDialog()
			val b = Bundle(1)
			b.putString(ARG_KEY, key)
			fragment.arguments = b
			return fragment
		}
	}
}
