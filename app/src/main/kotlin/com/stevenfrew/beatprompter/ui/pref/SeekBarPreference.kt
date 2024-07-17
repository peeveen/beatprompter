package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference

class SeekBarPreference(
	context: Context?,
	attrs: AttributeSet?,
	defStyleAttr: Int,
	defStyleRes: Int
) : DialogPreference(context!!, attrs, defStyleAttr, defStyleRes) {
	// Don't be fooled by the IDE. This constructor is REQUIRED!!!!!
	constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0, 0)

	private var mDefaultValue: Int = 0

	val suffix: String
	val offset: Int
	val max: Int

	init {
		val suffixResource =
			attrs?.getAttributeResourceValue(SettingsFragment.STEVEN_FREW_NAMESPACE, "suffix", 0)
		val maxResource =
			attrs?.getAttributeResourceValue(SettingsFragment.STEVEN_FREW_NAMESPACE, "max", 0)
		val offsetResource =
			attrs?.getAttributeResourceValue(SettingsFragment.STEVEN_FREW_NAMESPACE, "offset", 0)
		suffix =
			if (suffixResource == null || suffixResource == 0) "" else context?.getString(suffixResource)
				?: ""
		max =
			if (maxResource == null || maxResource == 0) 0 else context?.getString(maxResource)?.toInt()
				?: 0
		offset =
			if (offsetResource == null || offsetResource == 0) 0 else context?.getString(offsetResource)
				?.toInt() ?: 0
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		mDefaultValue = a.getString(index)!!.toInt()
		return mDefaultValue
	}

	override fun onSetInitialValue(defaultValue: Any?) {
		// Set default state from the XML attribute
		if (defaultValue is Int)
			preferenceValue = defaultValue
	}

	var preferenceValue: Int
		get() = getPersistedInt(mDefaultValue)
		set(value) {
			persistInt(value)
		}
}
