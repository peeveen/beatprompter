package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.stevenfrew.beatprompter.R

class PermissionsPreference : Preference {
	private val mPermissions: Array<out String>

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
		val arrayResource =
			attrs?.getAttributeResourceValue(SettingsFragment.StevenFrewNamespace, "permissions", 0) ?: 0
		mPermissions = context.resources.getStringArray(arrayResource)
	}

	override fun onBindViewHolder(view: PreferenceViewHolder) {
		if (permissionsGranted(context, mPermissions))
			view.findViewById(R.id.permissionDenied).visibility = View.GONE
		else
			view.findViewById(R.id.permissionGranted).visibility = View.GONE
		super.onBindViewHolder(view)
	}

	companion object {
		fun permissionsGranted(context: Context, permissions: Array<out String>): Boolean {
			return permissions.all {
				ContextCompat.checkSelfPermission(
					context,
					it
				) == PackageManager.PERMISSION_GRANTED
			}
		}
	}
}