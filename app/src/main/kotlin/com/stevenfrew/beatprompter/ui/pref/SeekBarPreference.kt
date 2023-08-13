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
		val suffixResource=attrs?.getAttributeResourceValue(sfns, "suffix", 0)
		val maxResource=attrs?.getAttributeResourceValue(sfns, "max", 0)
		val offsetResource=attrs?.getAttributeResourceValue(sfns, "offset", 0)
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

    companion object {
        private const val androidns = "http://schemas.android.com/apk/res/android"
        private const val sfns = "http://com.stevenfrew/"

        private fun getResourceString(context: Context, attrs: AttributeSet, identifier: String): String? {
            val resourceId = attrs.getAttributeResourceValue(androidns, identifier, 0)
            return if (resourceId == 0)
                attrs.getAttributeValue(androidns, identifier)
            else
                context.getString(resourceId)
        }
    }
}
