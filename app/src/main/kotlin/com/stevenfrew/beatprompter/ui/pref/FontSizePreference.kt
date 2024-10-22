package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.stevenfrew.beatprompter.BeatPrompter

class FontSizePreference(
	context: Context?,
	attrs: AttributeSet?,
	defStyleAttr: Int,
	defStyleRes: Int
) : DialogPreference(context!!, attrs, defStyleAttr, defStyleRes) {
	// Don't be fooled by the IDE. This constructor is REQUIRED!!!
	constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0, 0)

	private var defaultValue: Int = 0

	override fun onSetInitialValue(defaultValue: Any?) {
		// Set default state from the XML attribute
		if (defaultValue is Int)
			fontSize = defaultValue
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		defaultValue = a.getString(index)!!.toInt()
		return defaultValue
	}

	var fontSize: Int
		get() = getPersistedInt(defaultValue)
		set(value) {
			persistInt(value)
		}

	companion object {
		val FONT_SIZE_OFFSET: Int = BeatPrompter.fontManager.minimumFontSize.toInt()
		val FONT_SIZE_MAX: Int =
			(BeatPrompter.fontManager.maximumFontSize - BeatPrompter.fontManager.minimumFontSize).toInt()
	}
}
