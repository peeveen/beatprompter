package com.stevenfrew.beatprompter.ui.pref

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events


class SettingsActivity : AppCompatActivity(),
	PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
	private val fragment = SettingsFragment()

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, fragment)
			.commit()
	}

	override fun onPreferenceStartScreen(
		caller: PreferenceFragmentCompat,
		preferenceScreen: PreferenceScreen
	): Boolean {

		val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
		val args = Bundle()
		args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.key)
		fragment.arguments = args
		ft.replace(android.R.id.content, fragment, preferenceScreen.key)
		ft.addToBackStack(preferenceScreen.key)
		ft.commit()
		return true
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray
	) {
		EventRouter.sendEventToSettings(Events.PERMISSIONS_UPDATED)
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}
}
