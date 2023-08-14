package com.stevenfrew.beatprompter.ui.pref

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.PreferenceDialogFragmentCompat
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.util.Utils
import java.util.Locale


class FontSizePreferenceDialog : PreferenceDialogFragmentCompat(), SeekBar.OnSeekBarChangeListener {
	private var mSeekBar: SeekBar? = null
	private var mTextView: TextView? = null
	private var mCurrentValue = -1

	override fun onBindDialogView(view: View) {
		mSeekBar = view.findViewById(R.id.fontSizeSeekBar)
		mTextView = view.findViewById(R.id.fontSizeTextView)
		mSeekBar!!.setOnSeekBarChangeListener(this)
		mCurrentValue = (this.preference as FontSizePreference).getFontSize()
		mSeekBar!!.progress = mCurrentValue
		mTextView!!.text = String.format(Locale.getDefault(), "%d", mCurrentValue)
		//mSeekBar!!.min= FontSizePreference.FONT_SIZE_MIN
		mSeekBar!!.max = FontSizePreference.FONT_SIZE_MAX
		super.onBindDialogView(view)
	}

	override fun onDialogClosed(positiveResult: Boolean) {
		// When the user selects "OK", persist the new value
		if (positiveResult) {
			val value = mSeekBar!!.progress
			(this.preference as FontSizePreference).setFontSize(value)
		}
	}

	override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
		mCurrentValue = progress
		val size = mCurrentValue + FontSizePreference.FONT_SIZE_OFFSET
		mTextView!!.text = String.format(Locale.getDefault(), "%d", size)
		mTextView!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * Utils.FONT_SCALING)
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
