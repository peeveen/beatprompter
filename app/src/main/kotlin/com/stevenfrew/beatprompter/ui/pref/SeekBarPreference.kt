package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.content.res.TypedArray
import androidx.preference.DialogPreference
import android.util.AttributeSet

class SeekBarPreference : DialogPreference {
	private var mDefaultValue:Int=0

	val suffix:String
	val offset:Int
	val max:Int

	constructor(context: Context?):	this(context, null)

	constructor(context: Context?, attrs: AttributeSet?):this(context, attrs, 0)

	constructor(	context: Context?, attrs: AttributeSet?,	defStyleAttr: Int	):this(context, attrs, defStyleAttr, defStyleAttr)

	constructor(
		context: Context?, attrs: AttributeSet?,
		defStyleAttr: Int, defStyleRes: Int
	) :	super(context!!, attrs, defStyleAttr, defStyleRes){
		val suffixResource=attrs?.getAttributeResourceValue(SettingsFragment.StevenFrewNamespace, "suffix", 0)
		val maxResource=attrs?.getAttributeResourceValue(SettingsFragment.StevenFrewNamespace, "max", 0)
		val offsetResource=attrs?.getAttributeResourceValue(SettingsFragment.StevenFrewNamespace, "offset", 0)
		suffix = if(suffixResource==null || suffixResource==0) "" else context.getString(suffixResource)
		max = if(maxResource==null || maxResource==0) 0 else context.getString(maxResource).toInt()
		offset = if(offsetResource==null || offsetResource==0) 0 else context.getString(offsetResource).toInt()
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
			mDefaultValue=a.getString(index)!!.toInt()
			return mDefaultValue
		}

    override fun onSetInitialValue(defaultValue: Any?) {
			// Set default state from the XML attribute
			if(defaultValue is Int)
				setPreferenceValue(defaultValue)
    }

		fun setPreferenceValue(value:Int) {
			persistInt(value)
		}

		fun getPreferenceValue():Int{
			return getPersistedInt(mDefaultValue)
		}
}
