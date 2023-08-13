package com.stevenfrew.beatprompter.ui.pref

import android.app.AlertDialog
import android.content.Context
import androidx.preference.ListPreference
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.preference.PreferenceViewHolder
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.ImageArrayAdapter

class ImageListPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {
    private lateinit var resourceIds: IntArray

    private var inForceUpdate = false

    init {
        val typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.ImageListPreference)

        val indexCount = typedArray.indexCount
        val resources = context.resources
        val packageName = context.packageName
        if (indexCount > 0) {
            val imageNames = resources.getStringArray(
                    typedArray.getResourceId(indexCount - 1, -1))

            resourceIds = IntArray(imageNames.size)

            for (i in imageNames.indices) {
                val imageName = imageNames[i].substring(
                        imageNames[i].lastIndexOf('/') + 1,
                        imageNames[i].lastIndexOf('.'))

                resourceIds[i] = resources.getIdentifier(imageName,
                        "drawable", packageName)
            }

            typedArray.recycle()
        }
    }

    public override fun onBindViewHolder(view: PreferenceViewHolder) {
        super.onBindViewHolder(view)
        val imageView = view.findViewById(R.id.iconImageView) as ImageView
        val iconResource = when (Preferences.getStringPreference(key, "")) {
            BeatPrompter.getResourceString(R.string.googleDriveValue) -> R.drawable.ic_google_drive
            BeatPrompter.getResourceString(R.string.dropboxValue) -> R.drawable.ic_dropbox
            BeatPrompter.getResourceString(R.string.oneDriveValue) -> R.drawable.ic_onedrive
            BeatPrompter.getResourceString(R.string.localStorageValue) -> R.drawable.ic_device
            BeatPrompter.getResourceString(R.string.midi_usb_on_the_go_value) -> R.drawable.ic_usb
            BeatPrompter.getResourceString(R.string.midi_native_value) -> R.drawable.midi
            BeatPrompter.getResourceString(R.string.midi_bluetooth_value) -> R.drawable.ic_bluetooth
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

	/*
    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        val index = findIndexOfValue(sharedPreferences.getString(
                key, "1"))

        val listAdapter = ImageArrayAdapter(context,
                R.layout.imagelistitem, entries, resourceIds, index)

        // Order matters.
        builder.setAdapter(listAdapter, this)
        super.onPrepareDialogBuilder(builder)
    }
	 */
}
