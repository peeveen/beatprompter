package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R

class ImageListPreference(private val mContext: Context, attrs: AttributeSet) :
	ListPreference(mContext, attrs) {
	private lateinit var resourceIds: IntArray

	private var inForceUpdate = false

	init {
		val typedArray = context.obtainStyledAttributes(
			attrs,
			R.styleable.ImageListPreference
		)

		entries = getFilteredCloudStorageEntries()
		entryValues = getFilteredCloudStorageEntryValues()

		val indexCount = typedArray.indexCount
		val resources = context.resources
		val packageName = context.packageName
		if (indexCount > 0) {
			val imageNames = resources.getStringArray(
				typedArray.getResourceId(indexCount - 1, -1)
			)

			resourceIds = IntArray(imageNames.size)

			for (i in imageNames.indices) {
				val imageName = imageNames[i].substring(
					imageNames[i].lastIndexOf('/') + 1,
					imageNames[i].lastIndexOf('.')
				)

				resourceIds[i] = resources.getIdentifier(
					imageName,
					"drawable", packageName
				)
			}

			typedArray.recycle()
		}
	}

	private fun localStoragePermissionGranted(): Boolean =
		PermissionsPreference.permissionsGranted(
			mContext,
			mContext.resources.getStringArray(R.array.storage_permissions)
		)

	private fun googleServicesAvailable(): Boolean = GoogleApiAvailability.getInstance()
		.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

	private fun getFilteredCloudStorageEntries(): Array<CharSequence> {
		val localStorageFilteredEntries =
			if (localStoragePermissionGranted()) entries else entries.filter {
				it != mContext.getString(
					R.string.local_storage_string
				)
			}.toTypedArray()
		val googleDriveFilteredEntries =
			if (googleServicesAvailable()) localStorageFilteredEntries else localStorageFilteredEntries.filter {
				it != mContext.getString(
					R.string.google_drive_string
				)
			}.toTypedArray()
		return googleDriveFilteredEntries
	}

	private fun getFilteredCloudStorageEntryValues(): Array<CharSequence> {
		val localStorageFilteredEntries =
			if (localStoragePermissionGranted()) entryValues else entryValues.filter {
				it != mContext.getString(
					R.string.localStorageValue
				)
			}.toTypedArray()
		val googleDriveFilteredEntries =
			if (googleServicesAvailable()) localStorageFilteredEntries else localStorageFilteredEntries.filter {
				it != mContext.getString(
					R.string.googleDriveValue
				)
			}.toTypedArray()
		return googleDriveFilteredEntries
	}

	override fun onBindViewHolder(view: PreferenceViewHolder) {
		super.onBindViewHolder(view)
		val imageView = view.findViewById(R.id.iconImageView) as ImageView
		val iconResource = when (Preferences.getStringPreference(key, "")) {
			BeatPrompter.appResources.getString(R.string.googleDriveValue) -> R.drawable.ic_google_drive
			BeatPrompter.appResources.getString(R.string.dropboxValue) -> R.drawable.ic_dropbox
			BeatPrompter.appResources.getString(R.string.oneDriveValue) -> R.drawable.ic_onedrive
			BeatPrompter.appResources.getString(R.string.localStorageValue) -> R.drawable.ic_device
			BeatPrompter.appResources.getString(R.string.midi_usb_on_the_go_value) -> R.drawable.ic_usb
			BeatPrompter.appResources.getString(R.string.midi_native_value) -> R.drawable.midi
			BeatPrompter.appResources.getString(R.string.midi_bluetooth_value) -> R.drawable.ic_bluetooth
			BeatPrompter.appResources.getString(R.string.bluetoothModeBandLeaderValue) -> R.drawable.master0
			BeatPrompter.appResources.getString(R.string.bluetoothModeBandMemberValue) -> R.drawable.duncecap
			else -> R.drawable.blank_icon
		}
		imageView.setImageResource(iconResource)
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
		const val GooglePlayServicesPackageName = "com.android.market"
	}
}
