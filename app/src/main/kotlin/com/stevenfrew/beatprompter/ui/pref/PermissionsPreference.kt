package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.stevenfrew.beatprompter.R

class PermissionsPreference(
	context: Context?,
	attrs: AttributeSet?,
	defStyleAttr: Int,
	defStyleRes: Int
) : Preference(context!!, attrs, defStyleAttr, defStyleRes) {
	// Don't be fooled by the IDE. This constructor is REQUIRED!!!!!
	constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0, 0)

	private val permissions: Array<out String>
	private var inForceUpdate = false

	init {
		val arrayResource =
			attrs?.getAttributeResourceValue(SettingsFragment.STEVEN_FREW_NAMESPACE, "permissions", 0)
				?: 0
		permissions = context?.resources?.getStringArray(arrayResource) ?: arrayOf()
	}

	override fun onBindViewHolder(view: PreferenceViewHolder) {
		if (permissionsGranted(context, permissions)) {
			view.findViewById(R.id.permissionDenied).visibility = View.GONE
			view.findViewById(R.id.permissionGranted).visibility = View.VISIBLE
		} else {
			view.findViewById(R.id.permissionGranted).visibility = View.GONE
			view.findViewById(R.id.permissionDenied).visibility = View.VISIBLE
		}
		super.onBindViewHolder(view)
	}

	fun forceUpdate() {
		if (!inForceUpdate) {
			try {
				inForceUpdate = true
				notifyChanged()
			} finally {
				inForceUpdate = false
			}
		}
	}

	companion object {
		fun permissionsGranted(context: Context, permissions: Array<out String>): Boolean =
			permissions.all {
				ContextCompat.checkSelfPermission(
					context,
					it
				) == PackageManager.PERMISSION_GRANTED
			}
	}
}