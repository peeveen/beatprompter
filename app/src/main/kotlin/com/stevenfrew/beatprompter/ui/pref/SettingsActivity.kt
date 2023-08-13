package com.stevenfrew.beatprompter.ui.pref

import android.R
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.stevenfrew.beatprompter.storage.googledrive.GoogleDriveStorage


class SettingsActivity : AppCompatActivity(),
	PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
	private val mFragment = SettingsFragment()

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace(R.id.content, mFragment)
			.commit()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == GoogleDriveStorage.REQUEST_CODE_GOOGLE_SIGN_IN && resultCode == Activity.RESULT_OK) {
			GoogleDriveStorage.completeAction(this)
		}
	}

	override fun onPreferenceStartScreen(
		caller: PreferenceFragmentCompat,
		preferenceScreen: PreferenceScreen
	): Boolean {

		val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
		val args = Bundle()
		args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.key)
		mFragment.arguments = args
		ft.replace(R.id.content, mFragment, preferenceScreen.key)
		ft.addToBackStack(preferenceScreen.key)
		ft.commit()
		return true
	}
}
