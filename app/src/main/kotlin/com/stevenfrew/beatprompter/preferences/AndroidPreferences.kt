package com.stevenfrew.beatprompter.preferences

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Color
import androidx.preference.PreferenceManager
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.util.GlobalAppResources

class AndroidPreferences(
	private val appResources: GlobalAppResources,
	applicationContext: Context,
) : AbstractPreferences(appResources) {
	private val publicPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
	private val privatePreferences =
		applicationContext.getSharedPreferences(SHARED_PREFERENCES_ID, MODE_PRIVATE)

	init {
		PreferenceManager.setDefaultValues(applicationContext, R.xml.preferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.fontsizepreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.colorpreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.filepreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.midipreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.bluetoothpreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.permissionpreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.songdisplaypreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.audiopreferences, true)
		PreferenceManager.setDefaultValues(applicationContext, R.xml.songlistpreferences, true)
	}

	override fun getIntPreference(prefResourceString: Int, default: Int): Int =
		publicPreferences.getInt(appResources.getString(prefResourceString), default)

	override fun getStringPreference(key: String, default: String): String =
		publicPreferences.getString(key, default) ?: default

	override fun getStringSetPreference(key: String, default: Set<String>): Set<String> =
		publicPreferences.getStringSet(key, default) ?: default

	@Suppress("SameParameterValue")
	override fun getPrivateStringPreference(prefResourceString: Int, default: String): String =
		privatePreferences.getString(
			appResources.getString(prefResourceString),
			default
		) ?: default

	@Suppress("SameParameterValue")
	override fun getPrivateLongPreference(prefResourceString: Int, default: Long): Long =
		privatePreferences
			.getLong(
				appResources.getString(prefResourceString),
				default
			)

	override fun getStringPreference(prefResourceString: Int, default: String): String =
		publicPreferences.getString(
			appResources.getString(prefResourceString),
			default
		) ?: default

	override fun getStringSetPreference(prefResourceString: Int, default: Set<String>): Set<String> =
		publicPreferences.getStringSet(
			appResources.getString(prefResourceString),
			default
		) ?: default

	override fun getColorPreference(prefResourceString: Int, prefDefaultResourceString: Int): Int =
		publicPreferences.getInt(
			appResources.getString(prefResourceString),
			Color.parseColor(appResources.getString(prefDefaultResourceString))
		)

	override fun getBooleanPreference(prefResourceString: Int, default: Boolean): Boolean =
		publicPreferences.getBoolean(appResources.getString(prefResourceString), default)

	@Suppress("SameParameterValue")
	override fun setBooleanPreference(prefResourceString: Int, value: Boolean) =
		publicPreferences
			.edit()
			.putBoolean(appResources.getString(prefResourceString), value)
			.apply()

	override fun setStringPreference(prefResourceString: Int, value: String) =
		publicPreferences
			.edit()
			.putString(appResources.getString(prefResourceString), value)
			.apply()

	@Suppress("SameParameterValue")
	override fun setPrivateStringPreference(prefResourceString: Int, value: String) =
		privatePreferences
			.edit()
			.putString(appResources.getString(prefResourceString), value)
			.apply()

	@Suppress("SameParameterValue")
	override fun setPrivateLongPreference(prefResourceString: Int, value: Long) =
		privatePreferences
			.edit()
			.putLong(appResources.getString(prefResourceString), value)
			.apply()

	override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
		publicPreferences.registerOnSharedPreferenceChangeListener(listener)

	override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
		publicPreferences.unregisterOnSharedPreferenceChangeListener(listener)

	companion object {
		private const val SHARED_PREFERENCES_ID = "beatPrompterSharedPreferences"
	}
}