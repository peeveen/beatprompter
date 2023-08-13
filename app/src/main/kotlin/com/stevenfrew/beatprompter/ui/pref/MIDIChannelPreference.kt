package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference

class MIDIChannelPreference : DialogPreference {
	private var mDefaultValue: Int = 0
	val singleSelect: Boolean

	constructor(context: Context?) : this(context, null)

	constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

	constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : this(
		context,
		attrs,
		defStyleAttr,
		defStyleAttr
	)

	constructor(
		context: Context?, attrs: AttributeSet?,
		defStyleAttr: Int, defStyleRes: Int
	) : super(context!!, attrs, defStyleAttr, defStyleRes) {
		singleSelect =
			attrs?.getAttributeBooleanValue(SettingsFragment.StevenFrewNamespace, "singleSelect", false)
				?: false
	}

	fun setPreferenceValue(fontSize: Int) {
		persistInt(fontSize)
	}

	fun getPreferenceValue(): Int {
		return getPersistedInt(mDefaultValue)
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		mDefaultValue = a.getString(index)!!.toInt()
		return mDefaultValue
	}

	override fun onSetInitialValue(defaultValue: Any?) {
		if (defaultValue is Int)
			setPreferenceValue(defaultValue)
	}
}
